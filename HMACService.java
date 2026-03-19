import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/**
 * 
 * @author KFrancis05, help from claude.ai
 * 
 */
public class HMACService implements Runnable {
    private int tcpPort;
    private String serverIP;
    public HMACService(int tcpPort, String serverIP) {
        this.tcpPort = tcpPort;
        this.serverIP = serverIP;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("HMAC Service running on port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                String operation = in.readLine();
                String message = in.readLine();
                message = message.replace("\\n", "\n");
                String secretKey = in.readLine();
                String signature = in.readLine();
                String result = "";
                if (message == null || message.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                    result = "ERROR: No input provided";
                }
                else if (operation.equals("SIGN")) {
                    try {
                        Mac mac = Mac.getInstance("HmacSHA256");
                        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
                        mac.init(keySpec);
                        byte[] rawSignature = mac.doFinal(message.getBytes());
                        StringBuilder hex = new StringBuilder();
                        for (byte b : rawSignature) {
                            hex.append(String.format("%02x", b));
                        }
                        result = hex.toString();
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        result = "ERROR: " + e.getMessage();
                    }
                }
                else if (operation.equals("VERIFY")) {
                    try {
                        Mac mac = Mac.getInstance("HmacSHA256");
                        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
                        mac.init(keySpec);
                        byte[] rawSignature = mac.doFinal(message.getBytes());
                        StringBuilder hex = new StringBuilder();
                        for (byte b : rawSignature) {
                            hex.append(String.format("%02x", b));
                        }
                        String generatedSignature = hex.toString();
                        if (generatedSignature.equals(signature)) {
                            result = "VALID";
                        } else {
                            result = "INVALID";
                        }
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        result = "ERROR: " + e.getMessage();
                    }
                }
                else {
                    result = "ERROR: Invalid operation. Please send SIGN or VERIFY";
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
        new Thread(new HMACService(port, serverIP)).start();
        new Thread(new HeartbeatSender("node-HMAC", "HMAC", port, serverIP)).start();
        System.out.println("HMAC node started on port " + port);
    }
}