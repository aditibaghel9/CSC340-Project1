import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client4 {
    public static void main(String[] args) {
        try (Socket socket = new Socket("needs IP address for vm 4", 5050);
            //192.168.65.2
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                String msgToSend = scanner.nextLine();

                bufferedWriter.write(msgToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                String resp = bufferedReader.readLine();
                if (resp == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                System.out.println("Server: " + resp);

                if (msgToSend.equalsIgnoreCase("BYE")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}