

import java.net.*;
import java.util.*;

public class HeartbeatReceiver implements Runnable {

    private DatagramSocket socket;
    private volatile boolean running = true;
    private Map<String, Long> nodeLastSeen = new HashMap<>();

    public HeartbeatReceiver() {
        try {
            socket = new DatagramSocket(9999);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        long lastCheck = System.currentTimeMillis();

        while (running) {
            try {
                // Wait for incoming heartbeat packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Convert bytes to string
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + message);

                // Split the message by "|"
                String[] parts = message.split("\\|");

                // Make sure its a valid heartbeat message
                if (parts.length == 4 && parts[0].equals("HEARTBEAT")) {
                    String nodeName = parts[1];
                    String serviceName = parts[2];
                    int tcpPort = Integer.parseInt(parts[3]);

                    // Record the current time as last seen
                    nodeLastSeen.put(nodeName, System.currentTimeMillis());
                    System.out.println("Node alive: " + nodeName + " | Service: " + serviceName + " | Port: " + tcpPort);
                }

                // Check for dead nodes every 30 seconds
                if ((System.currentTimeMillis() - lastCheck) > 30000) {
                    checkForDeadNodes();
                    lastCheck = System.currentTimeMillis();
                }

            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }
    }

    public void checkForDeadNodes() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = nodeLastSeen.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String nodeName = entry.getKey();
            long lastSeen = entry.getValue();

            long secondsSinceLastSeen = (currentTime - lastSeen) / 1000;

            if (secondsSinceLastSeen > 120) {
                System.out.println("Node DEAD: " + nodeName + " (silent for " + secondsSinceLastSeen + " seconds)");
                iterator.remove();
            }
        }
    }

    public void stop() {
        running = false;
        socket.close();
    }
}