import java.io.*;
import java.net.*;

/**
 * Service Node - Runs on VMs
 * Can provide different services (CSV, IMAGE, etc.) based on command-line args
 * 
 * Usage: java ServiceNode <serviceName> <tcpPort> <serverIP> <nodeIP>
 * Example: java ServiceNode CSV 8010 192.168.64.1 192.168.64.5
 */
public class ServiceNode {
    private String serviceName;
    private int tcpPort;
    private String serverIP;
    private String nodeIP;
    
    public ServiceNode(String serviceName, int tcpPort, String serverIP, String nodeIP) {
        this.serviceName = serviceName.toUpperCase();
        this.tcpPort = tcpPort;
        this.serverIP = serverIP;
        this.nodeIP = nodeIP;
    }
    
    public void start() {
        System.out.println("=".repeat(60));
        System.out.println("SERVICE NODE: " + serviceName);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("Server IP: " + serverIP);
        System.out.println("Node IP: " + nodeIP);
        System.out.println("=".repeat(60));
        
        // Start heartbeat sender in background
        HeartbeatSender heartbeat = new HeartbeatSender(
            "node-" + serviceName,  // node name
            serviceName,             // service name
            tcpPort,                 // this node's TCP port
            serverIP                 // main server IP
        );
        new Thread(heartbeat).start();
        System.out.println("✓ Heartbeat sender started (sending to " + serverIP + ":9999)");
        
        // Start TCP server to receive tasks from main server
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("✓ Listening for tasks on port " + tcpPort);
            System.out.println("✓ Service Node ready!\n");
            
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println(">>> Main Server connected for task");
                handleTask(socket);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to start TCP server on port " + tcpPort);
            e.printStackTrace();
        }
    }
    
    private void handleTask(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()))) {
            
            // Read task data from main server
            String taskData = in.readLine();
            System.out.println(">>> Received task data: " + taskData);
            
            if (taskData == null || taskData.isEmpty()) {
                out.write("ERROR|No task data received");
                out.newLine();
                out.flush();
                return;
            }
            
            // Process based on service type
            String result;
            switch (serviceName) {
                case "CSV":
                    result = processCSV(taskData);
                    break;
                case "IMAGE":
                    result = processImage(taskData);
                    break;
                case "BASE64":
                    result = processBase64(taskData);
                    break;
                case "HMAC":
                    result = processHMAC(taskData);
                    break;
                case "Fifth List":
                    result = processEntropy(taskData);
                    break;
                default:
                    result = "ERROR|Unknown service: " + serviceName;
            }
            
            // Send result back to main server
            out.write(result);
            out.newLine();
            out.flush();
            System.out.println(">>> Task completed, result sent\n");
            
        } catch (IOException e) {
            System.err.println("ERROR handling task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String processCSV(String csvData) {
        try {
            System.out.println(">>> Processing CSV data...");
            CSVStatsService service = new CSVStatsService();
            String result = service.processCSV(csvData);
            System.out.println(">>> CSV processing complete");
            return "SUCCESS|" + result;
        } catch (Exception e) {
            return "ERROR|CSV processing failed: " + e.getMessage();
        }
    }
    
    private String processImage(String taskData) {
        try {
            System.out.println(">>> Processing IMAGE task...");
            // TODO: Implement image processing
            // For now, return placeholder
            return "SUCCESS|Image processed (placeholder - implement ImageTransformService)";
        } catch (Exception e) {
            return "ERROR|Image processing failed: " + e.getMessage();
        }
    }
    
    private String processBase64(String taskData) {
        try {
            System.out.println(">>> Processing BASE64 task...");
            // Parse: ENCODE|<text> or DECODE|<text>
            String[] parts = taskData.split("\\|", 2);
            String operation = parts[0];
            String data = parts.length > 1 ? parts[1] : "";
            
            if (operation.equalsIgnoreCase("ENCODE")) {
                String encoded = java.util.Base64.getEncoder().encodeToString(data.getBytes());
                return "SUCCESS|" + encoded;
            } else if (operation.equalsIgnoreCase("DECODE")) {
                byte[] decoded = java.util.Base64.getDecoder().decode(data);
                return "SUCCESS|" + new String(decoded);
            } else {
                return "ERROR|Unknown BASE64 operation: " + operation;
            }
        } catch (Exception e) {
            return "ERROR|Base64 processing failed: " + e.getMessage();
        }
    }
    
    private String processHMAC(String taskData) {
        try {
            System.out.println(">>> Processing HMAC task...");
            // TODO: Implement HMAC signing/verification
            return "SUCCESS|HMAC processed (placeholder - implement HMACService)";
        } catch (Exception e) {
            return "ERROR|HMAC processing failed: " + e.getMessage();
        }
    }
    
    private String processEntropy(String taskData) {
        try {
            System.out.println(">>> Processing ENTROPY task...");
            // TODO: Implement file entropy analysis
            return "SUCCESS|Entropy calculated (placeholder - implement EntropyService)";
        } catch (Exception e) {
            return "ERROR|Entropy processing failed: " + e.getMessage();
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java ServiceNode <serviceName> <tcpPort> <serverIP> <nodeIP>");
            System.out.println("Example: java ServiceNode CSV 8010 192.168.64.1 192.168.64.5");
            System.out.println("\nAvailable services: CSV, IMAGE, BASE64, HMAC, ENTROPY");
            return;
        }
        
        String serviceName = args[0];
        int tcpPort = Integer.parseInt(args[1]);
        String serverIP = args[2];
        String nodeIP = args[3];
        
        ServiceNode node = new ServiceNode(serviceName, tcpPort, serverIP, nodeIP);
        node.start();
    }
}