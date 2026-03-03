import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) {
        String serverIP = "192.168.64.1";
        int serverPort = 8000;
        
        try (Socket socket = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.println("Connected to Microservices Cluster");
            System.out.println("=====================================\n");
            
            // Request service list
            out.write("LIST");
            out.newLine();
            out.flush();
            
            String serviceList = in.readLine();
            System.out.println("Available services: " + serviceList);
            
            // Let user select service
            System.out.print("\nEnter service name (CSV, IMAGE, BASE64, etc.): ");
            String service = scanner.nextLine().toUpperCase();
            
            String taskRequest;
            
            // Handle IMAGE service differently
            if (service.equals("IMAGE")) {
                taskRequest = handleImageService(scanner);
            } else if (service.equals("CSV")) {
                taskRequest = handleCSVService(scanner);
            } else if (service.equals("BASE64")) {
                taskRequest = handleBase64Service(scanner);
            } else {
                // Generic handler for other services
                System.out.print("Enter data: ");
                String data = scanner.nextLine();
                taskRequest = "TASK|" + service + "|" + data;
            }
            
            // Send task
            out.write(taskRequest);
            out.newLine();
            out.flush();
            
            // Receive result
            String result = in.readLine();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("RESULT:");
            System.out.println("=".repeat(60));
            
            // Handle IMAGE result (save to file)
            if (service.equals("IMAGE") && result.startsWith("SUCCESS")) {
                handleImageResult(result);
            } else {
                System.out.println(result);
            }
            
            // Close connection
            out.write("BYE");
            out.newLine();
            out.flush();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handle IMAGE service - load image, select operation
     */
    private static String handleImageService(Scanner scanner) {
        System.out.println("\nIMAGE TRANSFORM SERVICE");
        System.out.println("Operations: RESIZE, ROTATE, GRAYSCALE, THUMBNAIL");
        System.out.print("Enter operation: ");
        String operation = scanner.nextLine().toUpperCase();
        
        System.out.print("Enter image file path: ");
        String imagePath = scanner.nextLine();
        
        try {
            // Convert image to base64
            String base64Image = ImageHelper.imageFileToBase64(imagePath);
            System.out.println("Image loaded (" + base64Image.length() + " chars)");
            
            String taskData;
            
            switch (operation) {
                case "RESIZE":
                    System.out.print("Enter width: ");
                    int width = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter height: ");
                    int height = Integer.parseInt(scanner.nextLine());
                    taskData = "RESIZE|" + width + "|" + height + "|" + base64Image;
                    break;
                    
                case "ROTATE":
                    System.out.print("Enter degrees (90, 180, 270, etc.): ");
                    int degrees = Integer.parseInt(scanner.nextLine());
                    taskData = "ROTATE|" + degrees + "|" + base64Image;
                    break;
                    
                case "GRAYSCALE":
                    taskData = "GRAYSCALE|" + base64Image;
                    break;
                    
                case "THUMBNAIL":
                    taskData = "THUMBNAIL|" + base64Image;
                    break;
                    
                default:
                    System.out.println("Unknown operation, using GRAYSCALE");
                    taskData = "GRAYSCALE|" + base64Image;
            }
            
            return "TASK|IMAGE|" + taskData;
            
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            return "TASK|IMAGE|ERROR";
        }
    }
    
    /**
     * Handle IMAGE result - extract and save image
     */
    private static void handleImageResult(String result) {
        try {
            // Result format: SUCCESS|message|base64ImageData
            String[] parts = result.split("\\|", 3);
            
            if (parts.length < 3) {
                System.out.println(result);
                return;
            }
            
            String status = parts[0];      // "SUCCESS"
            String message = parts[1];     // "Resized to 200x150"
            String base64Image = parts[2]; // base64 data
            
            System.out.println("Status: " + status);
            System.out.println("Message: " + message);
            
            // Save result image
            String outputPath = "result_image.jpg";
            ImageHelper.base64ToImageFile(base64Image, outputPath);
            System.out.println("Result saved to: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("Error handling result: " + e.getMessage());
            System.out.println("Raw result: " + result.substring(0, Math.min(100, result.length())));
        }
    }
    
    /**
     * Handle CSV service - multi-line input
     */
    private static String handleCSVService(Scanner scanner) {
        System.out.println("\nCSV STATS SERVICE");
        System.out.println("Enter CSV data (empty line to finish):");
        
        StringBuilder csvData = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) break;
            csvData.append(line).append("\n");
        }
        
        return "TASK|CSV|" + csvData.toString();
    }
    
    /**
     * Handle BASE64 service
     */
    private static String handleBase64Service(Scanner scanner) {
        System.out.println("\nBASE64 SERVICE");
        System.out.print("Operation (ENCODE/DECODE): ");
        String operation = scanner.nextLine().toUpperCase();
        
        System.out.print("Enter text: ");
        String text = scanner.nextLine();
        
        return "TASK|BASE64|" + operation + "|" + text;
    }
}