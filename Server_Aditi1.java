import java.io.*;
import java.net.*;

public class Server_Aditi{
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5050)) {
            System.out.println("Server started on port 5050...");
            //
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                    while (true) {
                        String msgFromClient = bufferedReader.readLine();

                        if (msgFromClient == null) {
                            System.out.println("Client disconnected.");
                            break;
                        }

                        System.out.println("Client: " + msgFromClient);

                        bufferedWriter.write("MSG Received");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        if (msgFromClient.equalsIgnoreCase("BYE")) {
                            System.out.println("Closing connection (BYE received).");
                            break;
                        }
                    }
                } finally {
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
