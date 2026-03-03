import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * ClientHandler - Handles each client connection in a separate thread
 * Updated to use enhanced HeartbeatReceiver with full node information
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
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(clientSocket.getOutputStream()))) {
            
            System.out.println("[Client] Connected from: " + clientSocket.getRemoteSocketAddress());
            
            while (true) {
                String request = in.readLine();
                
                if (request == null) {
                    System.out.println("[Client] Disconnected (null request)");
                    break;
                }
                
                System.out.println("[Client] Request: " + request);
                
                // Handle different commands
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
            System.out.println("[Client] Handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    private void handleListRequest(BufferedWriter out) throws IOException {
        // Get alive services from enhanced heartbeat receiver
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
        // Parse: TASK|CSV|<data>
        String[] parts = request.split("\\|", 3);
        
        if (parts.length < 3) {
            String error = "ERROR|400|Invalid task format. Use: TASK|SERVICE|DATA";
            out.write(error);
            out.newLine();
            out.flush();
            System.out.println("[Client] " + error);
            return;
        }
        
        String serviceName = parts[1].toUpperCase();
        String taskData = parts[2];
        
        System.out.println("[Client] Task request for service: " + serviceName);
        
        // Find the service node using enhanced receiver
        NodeInfo snInfo = heartbeatReceiver.getNodeInfoByService(serviceName);
        
        if (snInfo == null) {
            String error = "ERROR|404|Service " + serviceName + " is not currently available";
            out.write(error);
            out.newLine();
            out.flush();
            System.out.println("[Client] " + error);
            return;
        }
        
        System.out.println("[Client] Forwarding to: " + snInfo);
        
        // Forward task to service node
        String result = forwardToServiceNode(snInfo, taskData);
        
        // Send result back to client
        out.write(result);
        out.newLine();
        out.flush();
        System.out.println("[Client] Result sent: " + result.substring(0, Math.min(50, result.length())) + "...");
    }
    
    private String forwardToServiceNode(NodeInfo snInfo, String taskData) {
        try (Socket snSocket = new Socket(snInfo.ip, snInfo.port);
             BufferedWriter snOut = new BufferedWriter(
                new OutputStreamWriter(snSocket.getOutputStream()));
             BufferedReader snIn = new BufferedReader(
                new InputStreamReader(snSocket.getInputStream()))) {
            
            System.out.println("[Forward] Connecting to " + snInfo.ip + ":" + snInfo.port);
            
            // Send task data to service node
            snOut.write(taskData);
            snOut.newLine();
            snOut.flush();
            System.out.println("[Forward] Task sent to service node");
            
            // Set timeout for reading result (30 seconds)
            snSocket.setSoTimeout(30000);
            
            // Read result from service node
            String result = snIn.readLine();
            
            if (result == null) {
                return "ERROR|500|Service node returned no result";
            }
            
            System.out.println("[Forward] Result received from service node");
            return result;
            
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[Forward] Timeout waiting for service node response");
            return "ERROR|504|Service node timeout (30 seconds)";
        } catch (IOException e) {
            System.err.println("[Forward] Communication error: " + e.getMessage());
            return "ERROR|500|Failed to communicate with service node: " + e.getMessage();
        }
    }
}