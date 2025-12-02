package com.distributed.storage.metadata;

import com.distributed.storage.common.FileMetadata;
import com.distributed.storage.network.TCPClient;
import com.distributed.storage.network.TCPServer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataNode {
    public enum Role { HEAD, MIDDLE, TAIL, SINGLE }

    private TCPServer server;
    private ConcurrentHashMap<String, FileMetadata> metadataStore;
    private volatile boolean running;
    
    // Chain Topology
    private String nextNodeIp;
    private int nextNodePort;
    
    private String prevNodeIp;
    private int prevNodePort;
    
    private String skipToIp;
    private int skipToPort;
    
    private Role role;
    private int myPort;
    private java.util.concurrent.ExecutorService threadPool;

    public MetadataNode(String nextNodeIp, int nextNodePort) {
        this.server = new TCPServer();
        this.metadataStore = new ConcurrentHashMap<>();
        this.running = false;
        this.nextNodeIp = nextNodeIp;
        this.nextNodePort = nextNodePort;
        this.prevNodeIp = "";
        this.prevNodePort = -1;
        this.skipToIp = "";
        this.skipToPort = -1;
        this.threadPool = java.util.concurrent.Executors.newFixedThreadPool(50);
        
        if (nextNodePort == -1) {
            this.role = Role.TAIL;
        } else {
            this.role = Role.HEAD; // Default to HEAD, will change to MIDDLE if we get a predecessor
        }
    }

    public void start(int port) {
        this.myPort = port;
        if (!server.start(port)) {
            System.err.println("Failed to start metadata node on port " + port);
            return;
        }
        running = true;
        System.out.println("Metadata Node started on port " + port + " Role: " + role + " Next: " + nextNodePort);

        // Start Health Check Thread
        new Thread(this::healthCheckLoop).start();

        while (running) {
            int clientId = server.acceptClient();
            if (clientId != -1) {
                threadPool.submit(() -> handleClient(clientId));
            }
        }
    }
    
    private void healthCheckLoop() {
        while (running) {
            try {
                Thread.sleep(3000);
                if (nextNodePort != -1) {
                    if (!pingNext()) {
                        System.out.println("Port " + myPort + ": Next node " + nextNodePort + " failed!");
                        handleNextNodeFailure();
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private boolean pingNext() {
        TCPClient client = new TCPClient();
        if (!client.connect(nextNodeIp, nextNodePort)) return false;
        if (!client.sendMessage("PING")) {
            client.close();
            return false;
        }
        String resp = client.recvMessage();
        client.close();
        return "PONG".equals(resp);
    }
    
    private synchronized void handleNextNodeFailure() {
        if (skipToPort != -1) {
            System.out.println("Port " + myPort + ": Recovering using skip node -> " + skipToPort);
            // Update next to skip
            nextNodeIp = skipToIp;
            nextNodePort = skipToPort;
            
            // Consume skip
            skipToIp = "";
            skipToPort = -1;
            
            // Notify new next that I am prev
            notifyNextOfPredecessor();
        } else {
            System.out.println("Port " + myPort + ": No skip node. Becoming TAIL.");
            nextNodeIp = "";
            nextNodePort = -1;
            role = (prevNodePort == -1) ? Role.SINGLE : Role.TAIL;
        }
    }
    
    private void notifyNextOfPredecessor() {
        TCPClient client = new TCPClient();
        if (client.connect(nextNodeIp, nextNodePort)) {
            // UPDATE_PREV <myIp> <myPort>
            // We assume localhost for IP in this simplified version if not known, 
            // but let's try to send what we have.
            // Since we don't know our own IP easily without config, we'll send "127.0.0.1" for now as per test env.
            client.sendMessage("UPDATE_PREV 127.0.0.1 " + myPort);
            client.close();
        }
    }

    private void handleClient(int clientId) {
        while (running) {
            String command = server.recvMessage(clientId);
            if (command.isEmpty()) break;

            String[] parts = command.split(" ");
            String op = parts[0];

            switch (op) {
                case "PUT":
                    handlePut(clientId, command); // Pass full command to re-parse safely
                    break;
                case "GET":
                    if (parts.length == 2) handleGet(clientId, parts[1]);
                    break;
                case "PING":
                    server.sendMessage(clientId, "PONG");
                    break;
                case "UPDATE_PREV":
                    if (parts.length == 3) {
                        prevNodeIp = parts[1];
                        prevNodePort = Integer.parseInt(parts[2]);
                        if (role == Role.HEAD) role = Role.MIDDLE;
                        if (role == Role.SINGLE) role = Role.TAIL;
                        System.out.println("Port " + myPort + ": Updated prev to " + prevNodePort + ". New Role: " + role);
                        server.sendMessage(clientId, "ACK");
                    }
                    break;
                case "UPDATE_NEXT":
                    if (parts.length == 3) {
                        nextNodeIp = parts[1];
                        nextNodePort = Integer.parseInt(parts[2]);
                        if (role == Role.TAIL) role = Role.MIDDLE;
                        if (role == Role.SINGLE) role = Role.HEAD;
                        System.out.println("Port " + myPort + ": Updated next to " + nextNodePort + ". New Role: " + role);
                        server.sendMessage(clientId, "ACK");
                    }
                    break;
                case "SET_SKIP":
                    if (parts.length == 3) {
                        skipToIp = parts[1];
                        skipToPort = Integer.parseInt(parts[2]);
                        System.out.println("Port " + myPort + ": Set skip node to " + skipToPort);
                        server.sendMessage(clientId, "ACK");
                    }
                    break;
                case "GET_STATUS":
                    server.sendMessage(clientId, "ROLE=" + role + " NEXT=" + nextNodePort + " PREV=" + prevNodePort);
                    break;
                case "DIE":
                    System.out.println("Port " + myPort + ": Received DIE command. Stopping...");
                    running = false;
                    server.stop();
                    break;
                default:
                    server.sendMessage(clientId, "ERROR");
            }
        }
        server.closeClient(clientId);
    }

    private void handlePut(int clientId, String command) {
        try {
            // PUT filename size chunkSize totalChunks rootHash hash1,hash2...
            String[] parts = command.split(" ");
            // Basic validation
            if (parts.length < 7) {
                server.sendMessage(clientId, "ERROR_ARGS");
                return;
            }
            
            String filename = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            int chunkSize = Integer.parseInt(parts[3]);
            int totalChunks = Integer.parseInt(parts[4]);
            String rootHash = parts[5];
            String hashesStr = parts[6];
            
            FileMetadata meta = new FileMetadata();
            meta.filename = filename;
            meta.fileSize = fileSize;
            meta.chunkSize = chunkSize;
            meta.totalChunks = totalChunks;
            meta.rootHash = rootHash;
            meta.chunkHashes = Arrays.asList(hashesStr.split(","));

            // 1. Store locally
            metadataStore.put(filename, meta);
            System.out.println("Port " + myPort + ": Stored metadata for " + filename);

            // 2. Forward to next node if not tail
            boolean success = true;
            if (role != Role.TAIL && role != Role.SINGLE && nextNodePort != -1) {
                success = forwardPut(command);
            }

            // 3. Ack to client
            if (success) {
                server.sendMessage(clientId, "ACK");
            } else {
                server.sendMessage(clientId, "ERROR_FORWARD");
            }

        } catch (Exception e) {
            e.printStackTrace();
            server.sendMessage(clientId, "ERROR_PARSE");
        }
    }
    
    private boolean forwardPut(String command) {
        TCPClient client = new TCPClient();
        if (!client.connect(nextNodeIp, nextNodePort)) {
            System.err.println("Port " + myPort + ": Failed to forward to " + nextNodePort);
            return false;
        }
        
        if (!client.sendMessage(command)) {
            client.close();
            return false;
        }
        
        String response = client.recvMessage();
        client.close();
        return "ACK".equals(response);
    }

    private void handleGet(int clientId, String filename) {
        // Reads served by TAIL (or SINGLE)
        if (role != Role.TAIL && role != Role.SINGLE) {
            server.sendMessage(clientId, "REDIRECT_TO_TAIL"); 
            return;
        }

        FileMetadata meta = metadataStore.get(filename);
        if (meta == null) {
            server.sendMessage(clientId, "NOT_FOUND");
        } else {
            // Format: FOUND size chunkSize totalChunks rootHash hash1,hash2,...
            StringBuilder sb = new StringBuilder();
            sb.append("FOUND ");
            sb.append(meta.fileSize).append(" ");
            sb.append(meta.chunkSize).append(" ");
            sb.append(meta.totalChunks).append(" ");
            sb.append(meta.rootHash).append(" ");
            sb.append(String.join(",", meta.chunkHashes));
            
            server.sendMessage(clientId, sb.toString());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java MetadataNode <config_file> <node_id>");
            return;
        }
        
        String configFile = args[0];
        int nodeId = Integer.parseInt(args[1]);
        
        com.distributed.storage.common.NodeConfig config = new com.distributed.storage.common.NodeConfig(configFile, nodeId);
        com.distributed.storage.common.NodeInfo myNode = config.getMyNode();
        
        if (myNode == null) {
            System.err.println("Error: Node ID " + nodeId + " not found in config file.");
            return;
        }

        // Determine chain topology from config
        // We assume metadata nodes are those with IDs >= 11 (based on our sample config convention)
        // or we just look for the next node ID in the list that is also a metadata node.
        // For simplicity, let's assume the config list is sorted or we sort it.
        // And we assume the chain follows the order in the config.
        
        List<com.distributed.storage.common.NodeInfo> allNodes = config.getAllNodes();
        // Filter for metadata nodes (heuristic: ports 9000+) or just use specific IDs
        // For this implementation, let's assume IDs 11-20 are metadata nodes as per the sample config comment.
        List<com.distributed.storage.common.NodeInfo> metadataNodes = new java.util.ArrayList<>();
        for (com.distributed.storage.common.NodeInfo n : allNodes) {
             if (n.getId() >= 11) {
                 metadataNodes.add(n);
             }
        }
        
        // Sort by ID
        metadataNodes.sort(java.util.Comparator.comparingInt(com.distributed.storage.common.NodeInfo::getId));
        
        String nextIp = "";
        int nextPort = -1;
        
        for (int i = 0; i < metadataNodes.size(); i++) {
            if (metadataNodes.get(i).getId() == nodeId) {
                // Found myself. Next node is i+1
                if (i + 1 < metadataNodes.size()) {
                    com.distributed.storage.common.NodeInfo next = metadataNodes.get(i + 1);
                    nextIp = next.getHost();
                    nextPort = next.getPort();
                }
                break;
            }
        }
        
        new MetadataNode(nextIp, nextPort).start(myNode.getPort());
    }
}
