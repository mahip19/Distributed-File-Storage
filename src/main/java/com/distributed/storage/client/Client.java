package com.distributed.storage.client;

import com.distributed.storage.common.*;
import com.distributed.storage.dht.ConsistentHash;
import com.distributed.storage.network.TCPClient;
import com.distributed.storage.storage.StorageNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Client {
    private ConsistentHash dht;
    
    private String metadataHeadIp;
    private int metadataHeadPort;
    private String metadataTailIp;
    private int metadataTailPort;

    public Client(List<String> storageNodes, String metaHeadIp, int metaHeadPort, String metaTailIp, int metaTailPort) {
        dht = new ConsistentHash();
        for (String node : storageNodes) {
            dht.addNode(node);
        }
        this.metadataHeadIp = metaHeadIp;
        this.metadataHeadPort = metaHeadPort;
        this.metadataTailIp = metaTailIp;
        this.metadataTailPort = metaTailPort;
    }

    public void uploadFile(String filepath) {
        System.out.println("Uploading " + filepath);
        List<Chunk> chunks = FileUtils.splitFileIntoChunks(filepath);
        if (chunks.isEmpty()) {
            System.err.println("File is empty or not found");
            return;
        }
        HashUtils.hashAllChunks(chunks);
        
        List<String> hashes = new ArrayList<>();
        for (Chunk c : chunks) hashes.add(c.hash);
        String rootHash = HashUtils.computeRootHash(hashes);
        
        System.out.println("Root Hash (CID): " + rootHash);

        // 1. Upload chunks with replication
        int replicationFactor = 2; // k=2
        for (Chunk chunk : chunks) {
            List<String> nodes = dht.getNodesForKey(chunk.hash, replicationFactor);
            System.out.println("Chunk " + chunk.index + " -> " + nodes);
            
            int successCount = 0;
            for (String nodeAddr : nodes) {
                if (uploadChunkToNode(chunk, nodeAddr)) {
                    successCount++;
                } else {
                    System.err.println("  Failed to upload to " + nodeAddr);
                }
            }
            
            if (successCount == 0) {
                System.err.println("Failed to upload chunk " + chunk.index + " to any node!");
                return; // Abort
            }
        }
        
        // 2. Upload Metadata to HEAD
        if (putMetadata(filepath, chunks, rootHash)) {
            System.out.println("Metadata uploaded successfully.");
        } else {
            System.err.println("Failed to upload metadata.");
        }
        
        System.out.println("Upload complete.");
    }

    private boolean putMetadata(String filepath, List<Chunk> chunks, String rootHash) {
        TCPClient client = new TCPClient();
        if (!client.connect(metadataHeadIp, metadataHeadPort)) {
            return false;
        }
        
        File file = new File(filepath);
        long size = file.length();
        int chunkSize = FileUtils.CHUNK_SIZE;
        int totalChunks = chunks.size();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append(chunks.get(i).hash);
            if (i < chunks.size() - 1) sb.append(",");
        }
        
        // PUT filename size chunkSize totalChunks rootHash hash1,hash2...
        // Note: rootHash is String, so use %s
        String cmd = String.format("PUT %s %d %d %d %s %s", 
            file.getName(), size, chunkSize, totalChunks, rootHash, sb.toString());
            
        // Wait, the MetadataNode parsing expects: PUT filename size chunkSize totalChunks rootHash hash1,hash2...
        // My format string has an extra %d? No.
        // PUT %s(name) %d(size) %d(chunk) %d(total) %s(root) %s(hashes)
        // Wait, I passed 5 args to format but used 6 placeholders?
        // Ah, rootHash is string.
        // PUT %s %d %d %d %s %s
        
        cmd = "PUT " + file.getName() + " " + size + " " + chunkSize + " " + totalChunks + " " + rootHash + " " + sb.toString();

        if (!client.sendMessage(cmd)) {
            client.close();
            return false;
        }
        
        String response = client.recvMessage();
        client.close();
        return "ACK".equals(response);
    }
    
    public void downloadFile(String filename, String outputPath) {
        System.out.println("Downloading " + filename);
        
        // 1. Get Metadata from TAIL
        FileMetadata meta = getMetadata(filename);
        if (meta == null) {
            System.err.println("File not found in metadata.");
            return;
        }
        
        System.out.println("Metadata found. Root: " + meta.rootHash);
        
        // 2. Download chunks
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < meta.chunkHashes.size(); i++) {
            String hash = meta.chunkHashes.get(i);
            // Try to download from any replica
            List<String> nodes = dht.getNodesForKey(hash, 2);
            byte[] data = null;
            
            for (String node : nodes) {
                data = downloadChunkFromNode(hash, node);
                if (data != null) {
                    System.out.println("Retrieved chunk " + i + " from " + node);
                    break;
                }
            }
            
            if (data == null) {
                System.err.println("Failed to retrieve chunk " + i);
                return;
            }
            
            Chunk c = new Chunk();
            c.index = i;
            c.hash = hash;
            c.data = data;
            c.size = data.length;
            chunks.add(c);
        }
        
        // 3. Reconstruct
        if (FileUtils.reconstructFile(chunks, outputPath)) {
            System.out.println("File reconstructed at " + outputPath);
        } else {
            System.err.println("Reconstruction failed.");
        }
    }
    
    private FileMetadata getMetadata(String filename) {
        TCPClient client = new TCPClient();
        if (!client.connect(metadataTailIp, metadataTailPort)) {
            return null;
        }
        
        if (!client.sendMessage("GET " + filename)) {
            client.close();
            return null;
        }
        
        String response = client.recvMessage();
        client.close();
        
        if (response.startsWith("FOUND ")) {
            try {
                String[] parts = response.split(" ");
                FileMetadata meta = new FileMetadata();
                meta.fileSize = Long.parseLong(parts[1]);
                meta.chunkSize = Integer.parseInt(parts[2]);
                meta.totalChunks = Integer.parseInt(parts[3]);
                meta.rootHash = parts[4];
                meta.chunkHashes = Arrays.asList(parts[5].split(","));
                return meta;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean uploadChunkToNode(Chunk chunk, String nodeAddr) {
        String[] parts = nodeAddr.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        TCPClient client = new TCPClient();
        if (!client.connect(ip, port)) {
            return false;
        }

        if (!client.sendMessage("STORE " + chunk.hash)) {
            client.close();
            return false;
        }

        String response = client.recvMessage();
        if (!"READY".equals(response)) {
            client.close();
            return false;
        }

        if (!client.sendData(chunk.data)) {
            client.close();
            return false;
        }

        response = client.recvMessage();
        client.close();
        return "ACK".equals(response);
    }
    
    public byte[] downloadChunkFromNode(String hash, String nodeAddr) {
        String[] parts = nodeAddr.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        TCPClient client = new TCPClient();
        if (!client.connect(ip, port)) {
            return null;
        }

        if (!client.sendMessage("GET " + hash)) {
            client.close();
            return null;
        }

        String response = client.recvMessage();
        if ("FOUND".equals(response)) {
            byte[] data = client.recvData();
            client.close();
            return data;
        } else {
            client.close();
            return null;
        }
    }

    public static void main(String[] args) {
        // Full System Test with Failure Recovery
        
        // 1. Start Storage Nodes
        startStorageNode(8001);
        startStorageNode(8002);
        
        // 2. Start Metadata Nodes (Chain: 9001 -> 9002 -> 9003)
        // Tail (9003)
        startMetadataNode(9003, "", -1);
        try { Thread.sleep(500); } catch (Exception e) {}
        
        // Mid (9002) -> 9003
        startMetadataNode(9002, "127.0.0.1", 9003);
        try { Thread.sleep(500); } catch (Exception e) {}
        
        // Head (9001) -> 9002
        startMetadataNode(9001, "127.0.0.1", 9002);
        try { Thread.sleep(1000); } catch (Exception e) {}

        // 3. Configure Skip Node on Head (9001) to point to Tail (9003) in case Mid (9002) fails
        configureSkipNode(9001, "127.0.0.1", 9003);
        
        // 4. Initial Write (Before Failure)
        List<String> storageNodes = new ArrayList<>();
        storageNodes.add("127.0.0.1:8001");
        storageNodes.add("127.0.0.1:8002");
        
        Client client = new Client(storageNodes, "127.0.0.1", 9001, "127.0.0.1", 9003);
        
        String testFile = "test_upload.txt";
        try {
            Files.writeString(Path.of(testFile), "Initial content before failure.");
        } catch (IOException e) { e.printStackTrace(); }

        System.out.println("\n--- Initial Upload ---");
        client.uploadFile(testFile);
        
        // 5. Kill Middle Node (9002)
        System.out.println("\n--- KILLING MIDDLE NODE (9002) ---");
        killNode(9002);
        
        // 6. Wait for Head (9001) to detect failure (ping interval 3s)
        System.out.println("Waiting for failure detection (approx 5s)...");
        try { Thread.sleep(5000); } catch (InterruptedException e) {}
        
        // 7. Write New Content (Should skip 9002 and go 9001 -> 9003)
        System.out.println("\n--- Uploading Post-Failure Content ---");
        String testFile2 = "test_upload_2.txt";
        try {
            Files.writeString(Path.of(testFile2), "Content written after Middle Node failure.");
        } catch (IOException e) { e.printStackTrace(); }
        
        client.uploadFile(testFile2);
        
        // 8. Verify Read from Tail (9003)
        System.out.println("\n--- Verifying Read from Tail ---");
        String outFile2 = "test_downloaded_2.txt";
        client.downloadFile(testFile2, outFile2);
        
        try {
            String content = Files.readString(Path.of(outFile2));
            System.out.println("Content: " + content);
        } catch (IOException e) {}
        
        System.exit(0);
    }
    
    private static void configureSkipNode(int targetPort, String skipIp, int skipPort) {
        TCPClient client = new TCPClient();
        if (client.connect("127.0.0.1", targetPort)) {
            client.sendMessage("SET_SKIP " + skipIp + " " + skipPort);
            client.recvMessage(); // ACK
            client.close();
            System.out.println("Configured skip on " + targetPort + " -> " + skipPort);
        }
    }
    
    private static void killNode(int port) {
         TCPClient client = new TCPClient();
        if (client.connect("127.0.0.1", port)) {
            client.sendMessage("DIE");
            client.close();
            System.out.println("Sent DIE to " + port);
        }
    }
    
    private static void startStorageNode(int port) {
        new Thread(() -> {
            new StorageNode().start(port);
        }).start();
    }
    
    private static void startMetadataNode(int port, String nextIp, int nextPort) {
        new Thread(() -> {
            new com.distributed.storage.metadata.MetadataNode(nextIp, nextPort).start(port);
        }).start();
    }
}

