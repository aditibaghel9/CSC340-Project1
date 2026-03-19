import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * ClientHandler - Handles each client connection in a separate thread
 * @author aditibaghel9, KFrancis05, help from claude.ai
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private HeartbeatReceiver heartbeatReceiver;
    
    public ClientHandler(Socket socket, HeartbeatReceiver hbReceiver) {
        this.clientSocket = socket;
        this.heartbeatReceiver = hbReceiver;
    }
    
    @Override
    public void run() {
        try {
            clientSocket.setKeepAlive(true);
            clientSocket.setSoTimeout(0);
            clientSocket.setReceiveBufferSize(65536);
            clientSocket.setSendBufferSize(65536);
            clientSocket.setTcpNoDelay(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            System.out.println("[Client] Connected from: " + clientSocket.getRemoteSocketAddress());
            
            while (true) {
                String request = in.readLine();
                
                if (request == null) {
                    System.out.println("[Client] Disconnected (null request)");
                    break;
                }
                
                System.out.println("[Client] Request: " + request);
                
                if (request.equals("LIST")) {
                    handleListRequest(out);
                } else if (request.startsWith("TASK|")) {
                    handleTaskRequest(request, in, out);
                } else if (request.equalsIgnoreCase("BYE")) {
                    out.write("BYE|Goodbye from Microservices Cluster");
                    out.newLine();
                    out.flush();
                    System.out.println("[Client] Disconnected (BYE received)");
                    break;
                } else {
                    out.write("ERROR|400|Unknown command: " + request);
                    out.newLine();
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    private void handleListRequest(BufferedWriter out) throws IOException {
        List<String> aliveServices = heartbeatReceiver.getAliveServices();
        
        StringBuilder sb = new StringBuilder("SERVICES|");
        sb.append(aliveServices.size()).append("|");
        
        for (String service : aliveServices) {
            sb.append(service).append("|");
        }
        
        String response = sb.toString();
        out.write(response);
        out.newLine();
        out.flush();
        System.out.println("[Client] Sent service list: " + response);
    }
    
    private void handleTaskRequest(String request, BufferedReader in, BufferedWriter out) throws IOException {
        String[] parts = request.split("\\|", 3);
        
        if (parts.length < 2) {
            String error = "ERROR|400|Invalid task format. Use: TASK|SERVICE|DATA";
            out.write(error);
            out.newLine();
            out.flush();
            return;
        }
        
        String serviceName = parts[1].toUpperCase();
        String taskData = parts.length > 2 ? parts[2] : "";
        
        // Handle chunked CSV transfer
        if (serviceName.equals("CSV") && taskData.equals("CHUNKED")) {
            System.out.println("[Client] Receiving chunked CSV - streaming directly to service node...");

            NodeInfo snInfo = heartbeatReceiver.getNodeInfoByService("CSV");
            if (snInfo == null) {
                out.write("ERROR|404|CSV service not available");
                out.newLine();
                out.flush();
                // Drain remaining chunks from client
                String line;
                while ((line = in.readLine()) != null && !line.equals("END_CHUNKS")) {
                    out.write("ACK");
                    out.newLine();
                    out.flush();
                }
                return;
            }

            try {
                Socket snSocket = new Socket(snInfo.ip, snInfo.port);
                snSocket.setKeepAlive(true);
                snSocket.setSoTimeout(0);
                snSocket.setSendBufferSize(65536);
                snSocket.setReceiveBufferSize(65536);
                snSocket.setTcpNoDelay(true);

                BufferedWriter snOut = new BufferedWriter(new OutputStreamWriter(snSocket.getOutputStream()));
                BufferedReader snIn = new BufferedReader(new InputStreamReader(snSocket.getInputStream()));

                // Signal ready to client
                out.write("READY");
                out.newLine();
                out.flush();

                // Stream chunks directly to service node
                String line;
                int chunkCount = 0;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_CHUNKS")) {
                        break;
                    } else if (line.startsWith("CHUNK|")) {
                        snOut.write(line.substring(6));
                        snOut.newLine();
                        snOut.flush();
                        chunkCount++;

                        // Acknowledge to client
                        out.write("ACK");
                        out.newLine();
                        out.flush();
                    }
                }

                // Signal end to service node
                snOut.write("END_CSV");
                snOut.newLine();
                snOut.flush();

                System.out.println("[Client] Streamed " + chunkCount + " chunks to service node, waiting for result...");

                // Get result from service node
                String result = snIn.readLine();
                if (result == null) result = "ERROR|500|No result from service node";

                out.write(result);
                out.newLine();
                out.flush();

                snSocket.close();
                return;

            } catch (IOException e) {
                System.err.println("[Client] CSV streaming error: " + e.getMessage());
                out.write("ERROR|500|CSV streaming failed: " + e.getMessage());
                out.newLine();
                out.flush();
                return;
            }
        }

        System.out.println("[Client] Task request for service: " + serviceName);
    
        NodeInfo snInfo = heartbeatReceiver.getNodeInfoByService(serviceName);

        if (snInfo == null) {
            String error = "ERROR|404|Service " + serviceName + " is not currently available";
            out.write(error);
            out.newLine();
            out.flush();
            return;
        }

        System.out.println("[Client] Forwarding to: " + snInfo);
        String result = forwardToServiceNode(snInfo, taskData);

        out.write(result);
        out.newLine();
        out.flush();
        System.out.println("[Client] Result sent: " + result.substring(0, Math.min(50, result.length())) + "...");
    }
    
    private String forwardToServiceNode(NodeInfo snInfo, String taskData) {
        try {
            Socket snSocket = new Socket(snInfo.ip, snInfo.port);
            snSocket.setKeepAlive(true);
            snSocket.setSoTimeout(120000);
            snSocket.setTcpNoDelay(true);

            BufferedWriter snOut = new BufferedWriter(new OutputStreamWriter(snSocket.getOutputStream()));
            BufferedReader snIn = new BufferedReader(new InputStreamReader(snSocket.getInputStream()));

            System.out.println("[Forward] Connecting to " + snInfo.ip + ":" + snInfo.port);
            
            snOut.write(taskData);
            snOut.newLine();
            snOut.flush();
            System.out.println("[Forward] Task sent to service node");
            
            String result = snIn.readLine();
            
            if (result == null) {
                snSocket.close();
                return "ERROR|500|Service node returned no result";
            }
            
            System.out.println("[Forward] Result received from service node");
            snSocket.close();
            return result;
            
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[Forward] Timeout waiting for service node response");
            return "ERROR|504|Service node timeout";
        } catch (IOException e) {
            System.err.println("[Forward] Communication error: " + e.getMessage());
            return "ERROR|500|Failed to communicate with service node: " + e.getMessage();
        }
    }
}