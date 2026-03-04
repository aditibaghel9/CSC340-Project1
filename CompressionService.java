import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import java.util.Base64;
/**
 * 
 * @author KFrancis05, help from claude.ai and baeldung
 * 
 */
public class CompressionService implements Runnable {
    private int tcpPort;
    private String serverIP;
    public CompressionService(int tcpPort, String serverIP) {
        this.tcpPort = tcpPort;
        this.serverIP = serverIP;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Compression Service running on port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                String operation = in.readLine();
                String input = in.readLine();
                String result = "";
                if (operation == null || operation.isEmpty()) {
                    result = "ERROR: No operation provided";
                }
                else if (input == null || input.isEmpty()) {
                    result = "ERROR: No input provided";
                }
                else if (operation.equals("COMPRESS")) {
                    try {
                        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        GZIPOutputStream gzip = new GZIPOutputStream(byteStream);
                        gzip.write(bytes);
                        gzip.close();
                        byte[] compressedBytes = byteStream.toByteArray();
                        result = Base64.getEncoder().encodeToString(compressedBytes);
                    } catch (IOException e) {
                        result = "ERROR: Compression failed";
                    }
                }
                else if (operation.equals("DECOMPRESS")) {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(input);
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
                        GZIPInputStream gzip = new GZIPInputStream(byteStream);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = gzip.read(buffer)) > -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        gzip.close();
                        result = outputStream.toString();
                    } catch (IOException e) {
                        result = "ERROR: Invalid compressed data";
                    }
                }
                else {
                    result = "ERROR: Invalid operation. Please send COMPRESS or DECOMPRESS";
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
}