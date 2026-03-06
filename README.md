# QU Microservices Cluster

# Team Info
Team Members: Aditi Baghel and Kate Francis
Class: CSC340

# Project Outline 
Project Outline: This project consists of microservices cluster, which is a distributed networking system that enables clients to submit computational tasks to a dynamic pool of worker nodes without prior knowledge of their locations. One of the features this project shows is the system mirrors real-world service-oriented and microservice architectures by separating a control plane and a data plane. This project also consists of five service lists.

# Five Services (Service Nodes)

# VM1: CSV Statistics Service
IP: 192.168.64.5
Port: 8010 (TCP)
Implementation: CSVStatsService.java
Operations:
  - Calculate Mean 
  - Calculate Median 
  - Calculate Standard Deviation
  - Find Minimum value
  - Find Maximum value

# VM2: Image Transform Service
IP: 192.168.64.6
Port: 8020 (TCP)
Implementation: ImageTransformService.java
Tasks:
  - Resize images to specific dimensions
  - Rotate images by degrees (90°, 180°, 270°)
  - Convert to grayscale
  - Create thumbnails (150x150)

# VM3: Base64 Service
IP: 192.168.64.7
Port: 8030 (TCP)
Implementation: Inline in ServiceNode.java
Tasks:
  - Encode plain text to Base64
  - Decode Base64 to plain text

# VM4: HMAC Service
IP: 192.168.64.8
Port: 8040 (TCP)
Implementation: Inline in ServiceNode.java
Tasks:
  - Sign messages using HMAC-SHA256
  - Verify message signatures

# VM5: Compression Service
IP: 192.168.64.9
Port: 8050 (TCP)
Implementation: Inline in ServiceNode.java
Tasks:
  - Compress text using GZIP
  - Decompress GZIP data
  - Base64 encode compressed data



# System Architecture

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
                    │     MAIN SERVER (Mac)        │
                    │     192.168.64.1             │
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

# Communication Flow
1. Client connects to Main Server (TCP)
2. Client requests list of services
3. Server checks which Service Nodes are alive (heartbeats)
4. Client sends task to specific service
5. Server forwards task to Service Node
6. Service Node processes and returns result