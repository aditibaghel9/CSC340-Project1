# QU Microservices Cluster

## Team Info
Team Members: Aditi Baghel and Kate Francis
Class: CSC340

## Project Overview 
This project consists of microservices cluster, which is a distributed networking system that enables clients to submit computational tasks to a dynamic pool of worker nodes without prior knowledge of their locations. 

## Features
- the system mirrors real-world service-oriented and microservice architectures by separating a control plane and a data plane. 
- consists of five service lists.

## Five Services (Service Nodes)

### VM1: CSV Statistics Service
  IP: 192.168.64.5
  Port: 8010 (TCP)
  Implementation: CSVStatsService.java
  Operations:
    - Calculate Mean 
    - Calculate Median 
    - Calculate Standard Deviation
    - Find Minimum value
    - Find Maximum value

### VM2: Image Transform Service
  IP: 192.168.64.6
  Port: 8020 (TCP)
  Implementation: ImageTransformService.java
  Tasks:
    - Resize images to specific dimensions
    - Rotate images by degrees (90°, 180°, 270°)
    - Convert to grayscale
    - Create thumbnails (150x150)

### VM3: Base64 Service
  IP: 192.168.64.7
  Port: 8030 (TCP)
  Implementation: Inline in ServiceNode.java
  Tasks:
    - Encode plain text to Base64
    - Decode Base64 to plain text

### VM4: HMAC Service
  IP: 192.168.64.8
  Port: 8040 (TCP)
  Implementation: Inline in ServiceNode.java
  Tasks:
    - Sign messages using HMAC-SHA256
    - Verify message signatures

### VM5: Compression Service
  IP: 192.168.64.9
  Port: 8050 (TCP)
  Implementation: Inline in ServiceNode.java
  Tasks:
    - Compress text using GZIP
    - Decompress GZIP data
    - Base64 encode compressed data



## System Architecture

CLIENT LAYER
                    ┌─────────────────────┐
                    │   TCPClient.java    │
                    │  (User Interface)   │
                    └──────────┬──────────┘
                               │
                               │ TCP Connection
                               │ Port 8000
                               ↓
                    ┌──────────────────────────────┐
                    │     MAIN SERVER              │
                    │     192.168.56.1             │
                    │                              │
                    │  ┌────────────────────────┐  │
                    │  │  ClientHandler         │  │
                    │  │  (TCP Port 8000)       │  │
                    │  │  - Routes requests     │  │
                    │  │  - Multithreaded       │  │
                    │  └────────────────────────┘  │
                    │                              │
                    │  ┌────────────────────────┐  │
                    │  │  HeartbeatReceiver     │  │
                    │  │  (UDP Port 9999)       │  │
                    │  │  - Monitors health     │  │
                    │  │  - 120s timeout        │  │
                    │  └────────────────────────┘  │
                    └──┬────────────────────────┬──┘
                       │                        │
              TCP Tasks│                        │UDP Heartbeats
              (Forward)│                        │(Every 15-30s)
                       │                        │
       ┌───────────────┼────────────────────────┼───────────────┐
       │               │                        │               │
       ↓               ↓                        ↑               ↓
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│   VM1       │ │   VM2       │ │   VM3       │ │   VM4       │ │   VM5       │
│   CSV       │ │   IMAGE     │ │   BASE64    │ │   HMAC      │ │ COMPRESSION │
│             │ │             │ │             │ │             │ │             │
│ IP:         │ │ IP:         │ │ IP:         │ │ IP:         │ │ IP:         │
│ .64.5       │ │ .64.6       │ │ .64.7       │ │ .64.8       │ │ .64.9       │
│             │ │             │ │             │ │             │ │             │
│ Port:       │ │ Port:       │ │ Port:       │ │ Port:       │ │ Port:       │
│ 8010        │ │ 8020        │ │ 8030        │ │ 8040        │ │ 8050        │
│             │ │             │ │             │ │             │ │             │
│ Services:   │ │ Services:   │ │ Services:   │ │ Services:   │ │ Services:   │
│ • Mean      │ │ • Resize    │ │ • Encode    │ │ • Sign      │ │ • Compress  │
│ • Median    │ │ • Rotate    │ │ • Decode    │ │ • Verify    │ │ • Decompress│
│ • Std Dev   │ │ • Grayscale │ │             │ │             │ │             │
│ • Min       │ │ • Thumbnail │ │             │ │             │ │             │
│ • Max       │ │             │ │             │ │             │ │             │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘

## Communication Flow
1. Client connects to Main Server (TCP)
2. Client requests list of services
3. Server checks which Service Nodes are alive (heartbeats)
4. Client sends task to specific service
5. Server forwards task to Service Node
6. Service Node processes and returns result

---

## How to Run

### Prerequisites
- Java JDK 11 or higher
- 5 Ubuntu/Linux VMs (or run locally for testing)
- All machines on same network (192.168.64.x subnet)

---

### Step 1: Start Main Server 

  ### Navigate to project directory
  cd ~/MicroservicesProject

  ### Compile all files
  javac *.java

  ### Run main server
  java MainServer

  ### Expected Output
  
  Heartbeat receiver started on UDP 9999
  Main Server started on TCP 8000

### Step 2: Start Service Nodes (Each VM)

  ### VM1 - CSV Service:
  
  java ServiceNode CSV 8010 192.168.64.1 192.168.64.5
  

  ### VM2 - IMAGE Service:
  
  java ServiceNode IMAGE 8020 192.168.64.1 192.168.64.6

  ### VM3 - BASE64 Service:

  java ServiceNode BASE64 8030 192.168.64.1 192.168.64.7

  ### VM4 - HMAC Service:

  java ServiceNode HMAC 8040 192.168.64.1 192.168.64.8

  ### VM5 - COMPRESSION Service:

  java ServiceNode COMPRESSION 8050 192.168.64.1 192.168.64.9

  ### Expected Output (each VM):

  ============================================================
    SERVICE NODE: CSV
    TCP Port: 8010
    Server IP: 192.168.64.1
    Node IP: 192.168.64.5
  ============================================================
  ✓ Heartbeat sender started (sending to 192.168.64.1:9999)
  ✓ Listening for tasks on port 8010
  ✓ Service Node ready!

  Heartbeat sent: HEARTBEAT|node-CSV|CSV|8010

  ---

### Step 3: Run Client

  java TCPClient


  ### Example Session:
  
  Connected to Microservices Cluster
  =====================================

  Available services: SERVICES|5|CSV|IMAGE|BASE64|HMAC|COMPRESSION|

  Enter service name (CSV, IMAGE, BASE64, etc.): CSV

  CSV STATS SERVICE
  Enter CSV data (empty line to finish):
  Age,Height
  25,175
  30,180

  ============================================================
  RESULT:
  ============================================================
  SUCCESS|Column 1: mean=27.50, median=27.50, std=2.50, min=25.00, max=30.00
  Column 2: mean=177.50, median=177.50, std=2.50, min=175.00, max=180.00
  
---

## Protocol Specification

All messages use pipe delimiter (`|`) for field separation.

### Client → Server Commands

| Command | Format | Description | Example |
|---------|--------|-------------|---------|
| LIST | `LIST` | Request available services | `LIST` |
| TASK | `TASK\|<service>\|<data>` | Submit task to service | `TASK\|CSV\|Age,Height\n25,175` |
| BYE | `BYE` | Close connection | `BYE` |

### Server → Client Responses

| Response | Format | Description | Example |
|----------|--------|-------------|---------|
| SERVICES | `SERVICES\|<count>\|<svc1>\|<svc2>...` | List of alive services | `SERVICES\|5\|CSV\|IMAGE\|BASE64\|HMAC\|COMPRESSION\|` |
| SUCCESS | `SUCCESS\|<result>` | Task completed successfully | `SUCCESS\|Column 1: mean=27.50...` |
| ERROR | `ERROR\|<code>\|<message>` | Error occurred | `ERROR\|404\|Service not available` |

### Service Node → Server (UDP Heartbeat)

| Message | Format | Frequency |
|---------|--------|-----------|
| HEARTBEAT | `HEARTBEAT\|<node>\|<service>\|<port>` | Every 15-30 seconds (random) |

Example: HEARTBEAT|node-CSV|CSV|8010

---

## Fault Tolerance

### Heartbeat Mechanism
- Frequency: Service nodes send heartbeats every 15-30 seconds (randomized to prevent network congestion)
- Detection: Server marks node as DEAD after 120 seconds without heartbeat
- Recovery: Dead nodes automatically removed from service list; nodes automatically rejoin when restarted

### Failure Scenarios

Scenario 1: Service Node Crashes**
  1. Node stops sending heartbeats
  2. After 120 seconds, server removes node from registry
  3. Clients can no longer request that service
  4. Other services continue working normally

Scenario 2: Service Node Restarts**
  1. Node comes back online
  2. Immediately starts sending heartbeats
  3. Server adds node back to registry on first heartbeat
  4. Service becomes available within ~20 seconds
  5. No manual intervention required

Scenario 3: Task Timeout**
  - If service node doesn't respond within 30 seconds, client receives timeout error
  - Client can retry with same or different service

## Team Contributions

Aditi Baghel:
  - CSV Implementation
  - TCP Client
  - TCP Server
  - Image Implementation
  - ReadMe
  - Service Node

Kate Francis: 
  - HMAC Implementation
  - Base64 Implementation
  - Compression Implementation
  - UDP Client
  - UDP server
  - Heartbeat Receiver
  - NodeInfo Implementation

## Technologies Used

  - Java - Programming language
  - TCP Sockets - Client-server communication
  - UDP Sockets - Heartbeat protocol
  - Multithreading - Concurrent client handling
  - Java Standard Library - No external dependencies

## Libraries Used (Java Built-in)
  - java.net.* - Networking
  - java.io.* - Input/output
  - java.util.* - Data structures
  - javax.imageio.* - Image processing
  - java.awt.* - Graphics operations
  - javax.crypto.* - HMAC cryptography
  - java.util.zip.* - GZIP compression

---

##  File Structure

MicroservicesCluster/
├── MainServer.java              # Main coordinator server
├── ClientHandler.java           # TCP request handler (multithreaded)
├── HeartbeatReceiver.java       # UDP heartbeat listener
├── HeartbeatSender.java         # UDP heartbeat sender
├── NodeInfo.java                # Service node metadata
├── ServiceNode.java             # Generic service node (runs on VMs)
├── CSVStatsService.java         # CSV statistics implementation
├── ImageTransformService.java   # Image processing
├── ImageHelper.java             # Image encoding utilities
├── TCPClient.java               # Client application
├── TestImageService.java        # Image service test program
└── README.md                    # This file

---

## Network Configuration

| Machine | IP Address | Role | Ports |
|---------|-----------|------|-------|
| Server | 192.168.56.1 | Main Server | TCP: 8000, UDP: 9999 |
| VM1 | 192.168.64.5 | CSV Service | TCP: 8010 |
| VM2 | 192.168.64.6 | IMAGE Service | TCP: 8020 |
| VM3 | 192.168.64.7 | BASE64 Service | TCP: 8030 |
| VM4 | 192.168.64.8 | HMAC Service | TCP: 8040 |
| VM5 | 192.168.64.9 | COMPRESSION Service | TCP: 8050 |