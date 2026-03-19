import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.Scanner;
/**
 * TCP Client for Microservices Cluster
 * 
 * @author aditibaghel9, KFrancis05, help from claude.ai
 */
public class TCPClient {
    
    static String inputFileName = "";
    static String selectedOperation = "";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TCPClient <serverIP> <serverPort>");
            System.out.println("Example: java TCPClient 10.111.131.130 8000");
            return;
        }
        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        
        try {
            Socket socket = new Socket(serverIP, serverPort);
            socket.setKeepAlive(true);
            socket.setSoTimeout(0);
            socket.setSendBufferSize(65536);
            socket.setReceiveBufferSize(65536);
            socket.setTcpNoDelay(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            Scanner scanner = new Scanner(System.in);

            System.out.println("Connected to Microservices Cluster");
            System.out.println("=====================================\n");
            
            out.write("LIST");
            out.newLine();
            out.flush();

            String serviceList = in.readLine();
            System.out.println("Available services: " + serviceList);

            while (true) {
                System.out.print("\nEnter service name (CSV, IMAGE, BASE64, HMAC, COMPRESSION) or QUIT to exit: ");
                String service = scanner.nextLine().toUpperCase();
    
                if (service.equals("QUIT")) {
                    out.write("BYE");
                    out.newLine();
                    out.flush();
                    System.out.println("Goodbye!");
                    break;
                }
    
                String taskRequest;
    
                if (service.equals("IMAGE")) {
                    taskRequest = handleImageService(scanner);
                } else if (service.equals("CSV")) {
                    taskRequest = handleCSVService(scanner);
                } else if (service.equals("BASE64")) {
                    taskRequest = handleBase64Service(scanner);
                } else if (service.equals("HMAC")) {
                    taskRequest = handleHMACService(scanner);
                } else if (service.equals("COMPRESSION")) {
                    taskRequest = handleCompressionService(scanner);
                } else {
                    System.out.println("Unknown service, try again.");
                    continue;
                }
    
                if (taskRequest.startsWith("TASK|CSV|CHUNKED|")) {
                    String filePath = taskRequest.substring("TASK|CSV|CHUNKED|".length());
                    sendChunkedCSV(filePath, in, out);
                    String result = in.readLine();
                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("RESULT:");
                    System.out.println("=".repeat(60));
                    if (result != null && result.startsWith("SUCCESS")) {
                        handleCSVResult(result);
                    } else {
                        System.out.println(result);
                    }
                } else {
                    out.write(taskRequest);
                    out.newLine();
                    out.flush();
    
                    String result = in.readLine();
                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("RESULT:");
                    System.out.println("=".repeat(60));
    
                    if (service.equals("IMAGE") && result != null && result.startsWith("SUCCESS")) {
                        handleImageResult(result);
                    } else if (service.equals("BASE64") && result != null && result.startsWith("SUCCESS")) {
                        handleBase64Result(result);
                    } else if (service.equals("HMAC") && result != null && result.startsWith("SUCCESS")) {
                        handleHMACResult(result);
                    } else if (service.equals("COMPRESSION") && result != null && result.startsWith("SUCCESS")) {
                        handleCompressionResult(result);
                    } else if (service.equals("CSV") && result != null && result.startsWith("SUCCESS")) {
                        handleCSVResult(result);
                    } else {
                        System.out.println(result);
                    }
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String handleImageService(Scanner scanner) {
        System.out.println("\nIMAGE TRANSFORM SERVICE");
        System.out.println("Operations: RESIZE, ROTATE, GRAYSCALE, THUMBNAIL");
        System.out.print("Enter operation: ");
        selectedOperation = scanner.nextLine().toUpperCase();
        
        System.out.print("Enter image filename (from Downloads folder): ");
        String imageInput = scanner.nextLine().trim();
        
        String imagePath;
        String homeDir = System.getProperty("user.home");
        
        if (imageInput.startsWith("/") || imageInput.startsWith("~") || imageInput.contains(":\\")) {
            imagePath = imageInput;
            if (imagePath.startsWith("~")) {
                imagePath = imagePath.replace("~", homeDir);
            }
            inputFileName = Paths.get(imagePath).getFileName().toString();
        } else {
            imagePath = homeDir + "/Downloads/" + imageInput;
            inputFileName = imageInput;
        }
        
        System.out.println("Looking for image at: " + imagePath);
        
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("ERROR: Image file not found!");
            System.err.println("Expected location: " + imagePath);
            return "TASK|IMAGE|ERROR";
        }
        
        try {
            String base64Image = ImageHelper.imageFileToBase64(imagePath);
            System.out.println("✓ Image loaded successfully (" + base64Image.length() + " chars)");
            
            String taskData;
            switch (selectedOperation) {
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
    
    private static void handleImageResult(String result) {
        try {
            String[] parts = result.split("\\|", 3);
            if (parts.length < 3) {
                System.out.println(result);
                return;
            }
            System.out.println("Status: " + parts[0]);
            System.out.println("Message: " + parts[1]);
            
            String baseName = inputFileName.contains(".") 
                ? inputFileName.substring(0, inputFileName.lastIndexOf('.')) 
                : inputFileName;
            String ext = inputFileName.contains(".") 
                ? inputFileName.substring(inputFileName.lastIndexOf('.')) 
                : ".jpg";
            String outputPath = baseName + "_" + selectedOperation.toLowerCase() + ext;
            
            ImageHelper.base64ToImageFile(parts[2], outputPath);
            System.out.println("Result saved to: " + outputPath);
        } catch (Exception e) {
            System.err.println("Error handling result: " + e.getMessage());
        }
    }
    
    private static String handleCSVService(Scanner scanner) {
        System.out.println("\nCSV STATS SERVICE");
        System.out.println("Input method:");
        System.out.println("1. Type CSV data manually");
        System.out.println("2. Load from .csv file");
        System.out.print("Choice (1 or 2): ");
        String choice = scanner.nextLine();
    
        if (choice.equals("2")) {
            System.out.print("Enter CSV file path: ");
            String filePath = scanner.nextLine();
            inputFileName = Paths.get(filePath).getFileName().toString();
        
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("ERROR: File not found: " + filePath);
                return "TASK|CSV|ERROR";
            }
        
            long fileSizeBytes = file.length();
            System.out.println("File size: " + (fileSizeBytes / 1024) + " KB");
            return "TASK|CSV|CHUNKED|" + filePath;
        } else {
            inputFileName = "manual_input";
            System.out.println("Enter CSV data (empty line to finish):");
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = scanner.nextLine();
                if (line.isEmpty()) break;
                sb.append(line).append("\\n");
            }
            return "TASK|CSV|" + sb.toString();
        }
    }

    private static void sendChunkedCSV(String filePath, BufferedReader in, BufferedWriter out) {
        try {
            out.write("TASK|CSV|CHUNKED");
            out.newLine();
            out.flush();
        
            String ready = in.readLine();
            if (!"READY".equals(ready)) {
                System.err.println("Server not ready for chunks: " + ready);
                return;
            }
        
            System.out.println("Sending CSV data in chunks...");
            int chunkSize = 1000; // reduced chunk size for stability
            int lineCount = 0;
            int chunkCount = 0;
            StringBuilder chunk = new StringBuilder();
        
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    chunk.append(line).append("\\n");
                    lineCount++;
                
                    if (lineCount % chunkSize == 0) {
                        out.write("CHUNK|" + chunk.toString());
                        out.newLine();
                        out.flush();
                        chunk = new StringBuilder();
                        chunkCount++;

                        if (chunkCount % 1000 == 0) {
                            System.out.println("Sent chunk " + chunkCount + " (" + lineCount + " lines so far)");
                        }
                    
                        // Wait for acknowledgment with retry
                        String ack = in.readLine();
                        if (!"ACK".equals(ack)) {
                            System.err.println("Bad acknowledgment on chunk " + chunkCount + ": " + ack);
                            return;
                        }
                    }
                }
            }
        
            // Send remaining data
            if (chunk.length() > 0) {
                out.write("CHUNK|" + chunk.toString());
                out.newLine();
                out.flush();
                chunkCount++;
                String ack = in.readLine();
                if (!"ACK".equals(ack)) {
                    System.err.println("Bad acknowledgment on final chunk: " + ack);
                    return;
                }
            }
        
            out.write("END_CHUNKS");
            out.newLine();
            out.flush();
        
            System.out.println("✓ Sent " + lineCount + " lines in " + chunkCount + " chunks");
        
        } catch (IOException e) {
            System.err.println("Error sending chunked CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
 
    private static void handleCSVResult(String result) {
        try {
            String content = result.startsWith("SUCCESS|") ? result.substring(8) : result;
            System.out.println(content);
            
            String baseName = inputFileName.contains(".") 
                ? inputFileName.substring(0, inputFileName.lastIndexOf('.')) 
                : inputFileName;
            String outputPath = baseName + "_results.txt";
            
            Files.write(Paths.get(outputPath), content.getBytes());
            System.out.println("Results saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving results: " + e.getMessage());
        }
    }
    
    private static String handleBase64Service(Scanner scanner) {
        System.out.println("\nBASE64 SERVICE");
        System.out.print("Operation (ENCODE/DECODE): ");
        selectedOperation = scanner.nextLine().toUpperCase();
        
        System.out.println("Input method:");
        System.out.println("1. Type text manually");
        System.out.println("2. Load from .txt file");
        System.out.print("Choice (1 or 2): ");
        String choice = scanner.nextLine();
        
        String text;
        if (choice.equals("2")) {
            System.out.print("Enter file path: ");
            String filePath = scanner.nextLine();
            inputFileName = Paths.get(filePath).getFileName().toString();
            try {
                text = new String(Files.readAllBytes(Paths.get(filePath))).replace("\n", "\\n").replace("\r", "");
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                return "TASK|BASE64|ERROR";
            }
        } else {
            inputFileName = "manual_input";
            System.out.print("Enter text: ");
            text = scanner.nextLine();
        }
        
        return "TASK|BASE64|" + selectedOperation + "|" + text;
    }
    
    private static void handleBase64Result(String result) {
        try {
            String content = result.startsWith("SUCCESS|") ? result.substring(8) : result;
            content = content.replace("\\n", "\n");
            System.out.println(content);
            
            String outputPath = selectedOperation.equals("ENCODE") 
                ? "encoded_output.txt" 
                : "decoded_output.txt";
            
            Files.write(Paths.get(outputPath), content.getBytes());
            System.out.println("Result saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
    }
    
    private static String handleHMACService(Scanner scanner) {
        System.out.println("\nHMAC SERVICE");
        System.out.print("Operation (SIGN/VERIFY): ");
        selectedOperation = scanner.nextLine().toUpperCase();
        
        System.out.println("Input method:");
        System.out.println("1. Type message manually");
        System.out.println("2. Load from .txt file");
        System.out.print("Choice (1 or 2): ");
        String choice = scanner.nextLine();
        
        String message;
        if (choice.equals("2")) {
            System.out.print("Enter file path: ");
            String filePath = scanner.nextLine();
            inputFileName = Paths.get(filePath).getFileName().toString();
            try {
                message = new String(Files.readAllBytes(Paths.get(filePath))).replace("\n", "\\n").replace("\r", "");
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                return "TASK|HMAC|ERROR";
            }
        } else {
            inputFileName = "manual_input";
            System.out.print("Enter message: ");
            message = scanner.nextLine();
        }
        
        System.out.print("Enter secret key: ");
        String secretKey = scanner.nextLine();
        
        if (selectedOperation.equals("VERIFY")) {
            System.out.print("Enter signature to verify: ");
            String signature = scanner.nextLine();
            return "TASK|HMAC|" + selectedOperation + "|" + message + "|" + secretKey + "|" + signature;
        }
        
        return "TASK|HMAC|" + selectedOperation + "|" + message + "|" + secretKey;
    }
    
    private static void handleHMACResult(String result) {
        try {
            String content = result.startsWith("SUCCESS|") ? result.substring(8) : result;
            content = content.replace("\\n", "\n");
            System.out.println(content);
            
            if (selectedOperation.equals("SIGN")) {
                Files.write(Paths.get("hmac_signature.txt"), content.getBytes());
                System.out.println("Signature saved to: hmac_signature.txt");
            }
        } catch (IOException e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
    }
    
    private static String handleCompressionService(Scanner scanner) {
        System.out.println("\nCOMPRESSION SERVICE");
        System.out.print("Operation (COMPRESS/DECOMPRESS): ");
        selectedOperation = scanner.nextLine().toUpperCase();
        
        System.out.println("Input method:");
        System.out.println("1. Type text manually");
        System.out.println("2. Load from file");
        System.out.print("Choice (1 or 2): ");
        String choice = scanner.nextLine();
        
        String text;
        if (choice.equals("2")) {
            System.out.print("Enter file path: ");
            String filePath = scanner.nextLine();
            inputFileName = Paths.get(filePath).getFileName().toString();
            try {
                if (selectedOperation.equals("DECOMPRESS")) {
                    byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
                    text = Base64.getEncoder().encodeToString(fileBytes);
                } else {
                    text = new String(Files.readAllBytes(Paths.get(filePath))).replace("\n", "\\n").replace("\r", "");
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                return "TASK|COMPRESSION|ERROR";
            }
        } else {
            inputFileName = "manual_input";
            System.out.print("Enter text: ");
            text = scanner.nextLine();
        }
        
        return "TASK|COMPRESSION|" + selectedOperation + "|" + text;
    }
    
    private static void handleCompressionResult(String result) {
        try {
            String content = result.startsWith("SUCCESS|") ? result.substring(8) : result;
            System.out.println("Operation completed successfully");
            
            if (selectedOperation.equals("COMPRESS")) {
                byte[] compressedBytes = Base64.getDecoder().decode(content);
                String outputPath = inputFileName.equals("manual_input") 
                    ? "compressed_output.gz" 
                    : inputFileName + ".gz";
                Files.write(Paths.get(outputPath), compressedBytes);
                System.out.println("Compressed file saved to: " + outputPath);
                
            } else {
                String outputPath;
                if (inputFileName.equals("manual_input")) {
                    outputPath = "decompressed_output.txt";
                } else {
                    outputPath = inputFileName.endsWith(".gz") 
                        ? inputFileName.substring(0, inputFileName.length() - 3) 
                        : inputFileName + "_decompressed.txt";
                }
                content = content.replace("\\n", "\n");
                Files.write(Paths.get(outputPath), content.getBytes());
                System.out.println("Decompressed file saved to: " + outputPath);
            }
        } catch (IOException e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
    }
}