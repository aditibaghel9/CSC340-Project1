
import java.io.*;
import java.net.*;
import java.util.Base64;
/**
 * 
 * @author KFrancis05, help from claude.ai and baeldung
 *
 *  
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

                // Read input from server
                String operation = in.readLine(); // "ENCODE" or "DECODE"
                String input = in.readLine();      // the actual data

                String result = "";

                
                // 1. Take the input string
                // 2. If operation is "ENCODE", encode it
                // 3. If operation is "DECODE", decode it
                if(operation.equals("ENCODE")){
                    result = Base64.getEncoder().encodeToString(input.getBytes());
                }
                else if(operation.equals("DECODE")){
                    try {
                        byte[] decoded = Base64.getDecoder().decode(input);
                        result = new String(decoded);
                    } catch (IllegalArgumentException e) {
                        result = "ERROR: Invalid Base64 input";
                    }
                }

                // Send result back to server
                out.write(result);
                out.newLine();
                out.flush();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}