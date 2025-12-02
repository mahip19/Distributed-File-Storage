package com.distributed.storage.client;

import com.distributed.storage.client.VerifyFiles;
import com.distributed.storage.metadata.MetadataNode;
import com.distributed.storage.network.TCPClient;
import com.distributed.storage.storage.StorageNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SystemTests {

    public static void main(String[] args) {
        System.out.println("=== STARTING COMPREHENSIVE SYSTEM TESTS ===");
        
        try {
            testStorageFailure();
            testConcurrentClients();
            testBinaryFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("=== ALL TESTS COMPLETED ===");
        System.exit(0);
    }

    private static void testStorageFailure() throws Exception {
        System.out.println("\n[TEST] Storage Node Failure (Replication)");
        
        // 1. Setup Cluster
        startStorageNode(8001);
        startStorageNode(8002);
        startMetadataNode(9003, "", -1); // Tail
        Thread.sleep(500);
        startMetadataNode(9002, "127.0.0.1", 9003); // Mid
        Thread.sleep(500);
        startMetadataNode(9001, "127.0.0.1", 9002); // Head
        Thread.sleep(1000);

        List<String> storageNodes = List.of("127.0.0.1:8001", "127.0.0.1:8002");
        List<String> metadataNodes = List.of("127.0.0.1:9001", "127.0.0.1:9002", "127.0.0.1:9003");
        Client client = new Client(storageNodes, metadataNodes);

        // 2. Upload File
        String filename = "test_storage_fail.txt";
        Files.writeString(Path.of(filename), "Data that should survive storage node failure.");
        client.uploadFile(filename);

        // 3. Kill Storage Node 1 (8001)
        System.out.println(">>> Killing Storage Node 8001...");
        killNode(8001);
        Thread.sleep(1000);

        // 4. Download File (Should fetch from 8002)
        String outFilename = "test_storage_fail_out.txt";
        client.downloadFile(filename, outFilename);

        // 5. Verify
        String originalCID = VerifyFiles.computeCID(filename);
        String downloadedCID = VerifyFiles.computeCID(outFilename);
        
        if (originalCID.equals(downloadedCID)) {
            System.out.println("[PASS] Storage Failure Test: Integrity Verified.");
        } else {
            System.err.println("[FAIL] Storage Failure Test: Integrity Mismatch!");
        }
        
        // Teardown
        killNode(8002);
        killNode(9001);
        killNode(9002);
        killNode(9003);
        
        Files.deleteIfExists(Path.of(filename));
        Files.deleteIfExists(Path.of(outFilename));
        
        Thread.sleep(2000); // Wait for ports to free
    }

    private static void testConcurrentClients() throws Exception {
        System.out.println("\n[TEST] Concurrent Clients (Thread Pool)");

        // 1. Setup Cluster
        startStorageNode(8001);
        startStorageNode(8002);
        startMetadataNode(9003, "", -1);
        Thread.sleep(500);
        startMetadataNode(9002, "127.0.0.1", 9003);
        Thread.sleep(500);
        startMetadataNode(9001, "127.0.0.1", 9002);
        Thread.sleep(1000);

        List<String> storageNodes = List.of("127.0.0.1:8001", "127.0.0.1:8002");
        List<String> metadataNodes = List.of("127.0.0.1:9001", "127.0.0.1:9002", "127.0.0.1:9003");

        int clientCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String fname = "concurrent_" + id + ".txt";
                    Files.writeString(Path.of(fname), "Concurrent data " + id);
                    Client c = new Client(storageNodes, metadataNodes);
                    c.uploadFile(fname);
                    
                    String outName = "concurrent_out_" + id + ".txt";
                    c.downloadFile(fname, outName);
                    
                    if (!VerifyFiles.computeCID(fname).equals(VerifyFiles.computeCID(outName))) {
                        synchronized(failures) { failures.add("Integrity fail client " + id); }
                    }
                } catch (Exception e) {
                    synchronized(failures) { failures.add("Exception client " + id + ": " + e.getMessage()); }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        if (failures.isEmpty()) {
            System.out.println("[PASS] Concurrent Clients Test: All " + clientCount + " clients succeeded.");
        } else {
            System.err.println("[FAIL] Concurrent Clients Test: " + failures.size() + " failures.");
            failures.forEach(System.err::println);
        }
        
        // Teardown
        killNode(8001);
        killNode(8002);
        killNode(9001);
        killNode(9002);
        killNode(9003);
        
        // Cleanup Files
        for (int i = 0; i < clientCount; i++) {
            Files.deleteIfExists(Path.of("concurrent_" + i + ".txt"));
            Files.deleteIfExists(Path.of("concurrent_out_" + i + ".txt"));
        }
    }

    private static void testBinaryFiles() throws Exception {
        System.out.println("\n[TEST] Binary Files (Image & PDF)");

        // 1. Setup Cluster
        startStorageNode(8001);
        startStorageNode(8002);
        startMetadataNode(9003, "", -1);
        Thread.sleep(500);
        startMetadataNode(9002, "127.0.0.1", 9003);
        Thread.sleep(500);
        startMetadataNode(9001, "127.0.0.1", 9002);
        Thread.sleep(1000);

        List<String> storageNodes = List.of("127.0.0.1:8001", "127.0.0.1:8002");
        List<String> metadataNodes = List.of("127.0.0.1:9001", "127.0.0.1:9002", "127.0.0.1:9003");
        Client client = new Client(storageNodes, metadataNodes);

        String[] files = {"test_image.jpg", "test_pdf.pdf"};
        
        for (String filename : files) {
            if (!Files.exists(Path.of(filename))) {
                System.out.println("Skipping " + filename + " (File not found)");
                continue;
            }
            
            System.out.println(">>> Testing " + filename);
            client.uploadFile(filename);
            
            String outFilename = "restored_" + filename;
            client.downloadFile(filename, outFilename);
            
            String originalCID = VerifyFiles.computeCID(filename);
            String downloadedCID = VerifyFiles.computeCID(outFilename);
            
            if (originalCID.equals(downloadedCID)) {
                System.out.println("[PASS] " + filename + ": Integrity Verified (" + originalCID + ")");
            } else {
                System.err.println("[FAIL] " + filename + ": Integrity Mismatch!");
            }
            
            // Cleanup output
            Files.deleteIfExists(Path.of(outFilename));
        }

        // Teardown
        killNode(8001);
        killNode(8002);
        killNode(9001);
        killNode(9002);
        killNode(9003);
        Thread.sleep(2000);
    }

    // --- Helpers ---

    private static void startStorageNode(int port) {
        new Thread(() -> new StorageNode().start(port)).start();
    }

    private static void startMetadataNode(int port, String nextIp, int nextPort) {
        new Thread(() -> new MetadataNode(nextIp, nextPort).start(port)).start();
    }

    private static void killNode(int port) {
        TCPClient client = new TCPClient();
        if (client.connect("127.0.0.1", port)) {
            client.sendMessage("DIE");
            client.close();
        }
    }
}
