import java.io.*;
import java.net.*;
import java.util.Base64;
/**
 * 
 * @author KFrancis05, help from claude.ai and baeldung
 *
 */
public class Base64Service implements Runnable {
    private int tcpPort;
    private String serverIP;
    public Base64Service(int tcpPort, String serverIP) {
        this.tcpPort = tcpPort;
        this.serverIP = serverIP;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Base64 Service running on port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                String operation = in.readLine();
                String input = in.readLine();
                input = input.replace("\\n", "\n");
                String result = "";
                if (input == null || input.isEmpty()) {
                    result = "ERROR: No input provided";
                }
                else if (operation.equals("ENCODE")) {
                    result = Base64.getEncoder().encodeToString(input.getBytes());
                }
                else if (operation.equals("DECODE")) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(input);
                        result = new String(decoded);
                    } catch (IllegalArgumentException e) {
                        result = "ERROR: Invalid Base64 input";
                    }
                }
                else {
                    result = "ERROR: Invalid operation. Please send ENCODE or DECODE";
                }
                out.write(result);
                out.newLine();
                out.flush();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String serverIP = args[1];
        new Thread(new Base64Service(port, serverIP)).start();
        new Thread(new HeartbeatSender("node-BASE64", "BASE64", port, serverIP)).start();
        System.out.println("Base64 node started on port " + port);
    }
}