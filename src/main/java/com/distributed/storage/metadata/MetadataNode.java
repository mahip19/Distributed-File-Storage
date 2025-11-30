package com.distributed.storage.metadata;

import com.distributed.storage.common.FileMetadata;
import com.distributed.storage.network.TCPClient;
import com.distributed.storage.network.TCPServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataNode {
    private TCPServer server;
    private ConcurrentHashMap<String, FileMetadata> metadataStore;
    private boolean running;
    
    // Chain Replication
    private String nextNodeIp;
    private int nextNodePort;
    private boolean isTail;

    public MetadataNode(String nextNodeIp, int nextNodePort) {
        this.server = new TCPServer();
        this.metadataStore = new ConcurrentHashMap<>();
        this.running = false;
        this.nextNodeIp = nextNodeIp;
        this.nextNodePort = nextNodePort;
        this.isTail = (nextNodePort == -1);
    }

    public void start(int port) {
        if (!server.start(port)) {
            System.err.println("Failed to start metadata node on port " + port);
            return;
        }
        running = true;
        System.out.println("Metadata Node started on port " + port + (isTail ? " (TAIL)" : " -> " + nextNodeIp + ":" + nextNodePort));

        while (running) {
            int clientId = server.acceptClient();
            if (clientId != -1) {
                new Thread(() -> handleClient(clientId)).start();
            }
        }
    }

    private void handleClient(int clientId) {
        while (running) {
            String command = server.recvMessage(clientId);
            if (command.isEmpty()) break;

            // Simple protocol:
            // PUT <filename> <size> <chunkSize> <totalChunks> <rootHash> <hash1,hash2,...>
            // GET <filename>

            String[] parts = command.split(" ", 6); // Limit split for PUT
            String op = parts[0];

            if ("PUT".equals(op) && parts.length == 6) {
                handlePut(clientId, parts);
            } else if ("GET".equals(op) && parts.length == 2) {
                handleGet(clientId, parts[1]);
            } else {
                server.sendMessage(clientId, "ERROR");
            }
        }
        server.closeClient(clientId);
    }

    private void handlePut(int clientId, String[] parts) {
        try {
            String filename = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            int chunkSize = Integer.parseInt(parts[3]);
            int totalChunks = Integer.parseInt(parts[4]);
            String rootHash = parts[5].split(" ")[0]; // In case there are more spaces
            // The rest is hashes. Wait, split limit 6 puts all hashes in parts[5]?
            // Actually, let's parse carefully.
            // The command format: PUT filename size chunkSize totalChunks rootHash hash1,hash2,hash3
            
            // Re-parsing to be safe
            String[] allParts = String.join(" ", parts).split(" ");
            // This is messy if filename has spaces. Assume no spaces for now.
            
            filename = allParts[1];
            fileSize = Long.parseLong(allParts[2]);
            chunkSize = Integer.parseInt(allParts[3]);
            totalChunks = Integer.parseInt(allParts[4]);
            rootHash = allParts[5];
            String hashesStr = allParts[6];
            
            FileMetadata meta = new FileMetadata();
            meta.filename = filename;
            meta.fileSize = fileSize;
            meta.chunkSize = chunkSize;
            meta.totalChunks = totalChunks;
            meta.rootHash = rootHash;
            meta.chunkHashes = Arrays.asList(hashesStr.split(","));

            // 1. Store locally
            metadataStore.put(filename, meta);
            System.out.println("Stored metadata for " + filename);

            // 2. Forward to next node if not tail
            boolean success = true;
            if (!isTail) {
                success = forwardPut(allParts);
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
    
    private boolean forwardPut(String[] parts) {
        TCPClient client = new TCPClient();
        if (!client.connect(nextNodeIp, nextNodePort)) {
            System.err.println("Failed to connect to next node " + nextNodeIp + ":" + nextNodePort);
            return false;
        }
        
        // Reconstruct command
        String command = String.join(" ", parts);
        if (!client.sendMessage(command)) {
            client.close();
            return false;
        }
        
        String response = client.recvMessage();
        client.close();
        return "ACK".equals(response);
    }

    private void handleGet(int clientId, String filename) {
        // Only TAIL serves reads? Or any node?
        // Plan says: "Reads are served exclusively by the TAIL to guarantee consistency."
        if (!isTail) {
            server.sendMessage(clientId, "REDIRECT_TO_TAIL"); 
            // In a real system we'd tell them where tail is, but let's assume client knows for now
            // Or we could just proxy it.
            // Let's enforce the rule: if not tail, error.
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
        if (args.length < 1) {
            System.out.println("Usage: java MetadataNode <port> [nextIp nextPort]");
            return;
        }
        
        int port = Integer.parseInt(args[0]);
        String nextIp = "";
        int nextPort = -1;
        
        if (args.length >= 3) {
            nextIp = args[1];
            nextPort = Integer.parseInt(args[2]);
        }
        
        new MetadataNode(nextIp, nextPort).start(port);
    }
}
