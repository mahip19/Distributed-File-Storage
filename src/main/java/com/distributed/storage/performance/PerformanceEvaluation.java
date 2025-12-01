package com.distributed.storage.performance;

import com.distributed.storage.client.Client;
import com.distributed.storage.metadata.MetadataNode;
import com.distributed.storage.storage.StorageNode;
import com.distributed.storage.network.TCPClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PerformanceEvaluation {

    private static final String OUTPUT_FILE = "performance_evaluation.txt";
    private static final String TEST_DIR = "test_data";

    public static void main(String[] args) {
        // Create test directory
        new File(TEST_DIR).mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
            writer.println("Performance Evaluation Results");
            writer.println("==============================");
            writer.println("Date: " + java.time.LocalDateTime.now());
            writer.println();

            // 1. Start Cluster
            System.out.println("Starting Cluster...");
            startCluster();
            // Give it a moment to stabilize
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            // 2. Configure Client
            List<String> storageNodes = new ArrayList<>();
            storageNodes.add("127.0.0.1:8001");
            storageNodes.add("127.0.0.1:8002");

            List<String> metadataNodes = new ArrayList<>();
            metadataNodes.add("127.0.0.1:9001");
            metadataNodes.add("127.0.0.1:9002");
            metadataNodes.add("127.0.0.1:9003");

            Client client = new Client(storageNodes, metadataNodes);

            // 3. Run Tests
            // Test Case 1: Small File (100 KB)
            runTest(writer, client, "small_file.dat", 100 * 1024);

            // Test Case 2: Medium File (5 MB) - 5 Chunks
            runTest(writer, client, "medium_file.dat", 5 * 1024 * 1024);

            // Test Case 3: Large File (20 MB) - 20 Chunks
            runTest(writer, client, "large_file.dat", 20 * 1024 * 1024);

            System.out.println("Performance evaluation complete. Results saved to " + OUTPUT_FILE);
            
            // Force exit to kill threads
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runTest(PrintWriter writer, Client client, String filename, int sizeBytes) {
        System.out.println("\nRunning Test: " + filename + " (" + sizeBytes + " bytes)");
        String filepath = TEST_DIR + File.separator + filename;
        createDummyFile(filepath, sizeBytes);

        writer.println("Test Case: " + filename);
        writer.println("-------------------------------");
        writer.println("File Size: " + sizeBytes + " bytes");

        // Measure Upload
        long startUpload = System.currentTimeMillis();
        client.uploadFile(filepath);
        long endUpload = System.currentTimeMillis();
        long uploadLatency = endUpload - startUpload;
        
        double uploadThroughput = (double) sizeBytes / 1024 / 1024 / (uploadLatency / 1000.0); // MB/s

        writer.printf("Total Upload Latency: %d ms%n", uploadLatency);
        writer.printf("Upload Throughput: %.2f MB/s%n", uploadThroughput);
        writer.printf("Chunk Upload Latency: %d ms%n", client.lastChunkUploadDuration);
        writer.printf("Metadata Chain Latency: %d ms%n", client.lastMetadataUploadDuration);

        // Measure Download
        String downloadPath = TEST_DIR + File.separator + "downloaded_" + filename;
        long startDownload = System.currentTimeMillis();
        client.downloadFile(filename, downloadPath); // Note: Client.downloadFile takes filename (key) and outputPath
        long endDownload = System.currentTimeMillis();
        long downloadLatency = endDownload - startDownload;

        double downloadThroughput = (double) sizeBytes / 1024 / 1024 / (downloadLatency / 1000.0); // MB/s

        writer.printf("Total Download Latency: %d ms%n", downloadLatency);
        writer.printf("Download Throughput: %.2f MB/s%n", downloadThroughput);
        writer.println();
        writer.flush();
        
        // Clean up downloaded file
        new File(downloadPath).delete();
    }

    private static void createDummyFile(String filepath, int size) {
        try {
            byte[] data = new byte[size];
            new Random().nextBytes(data);
            Files.write(Path.of(filepath), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startCluster() {
        // Start Storage Nodes
        startStorageNode(8001);
        startStorageNode(8002);

        // Start Metadata Nodes (Chain: 9001 -> 9002 -> 9003)
        // Tail (9003)
        startMetadataNode(9003, "", -1);
        try { Thread.sleep(500); } catch (Exception e) {}

        // Mid (9002) -> 9003
        startMetadataNode(9002, "127.0.0.1", 9003);
        try { Thread.sleep(500); } catch (Exception e) {}

        // Head (9001) -> 9002
        startMetadataNode(9001, "127.0.0.1", 9002);
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    private static void startStorageNode(int port) {
        new Thread(() -> {
            new StorageNode().start(port);
        }).start();
    }

    private static void startMetadataNode(int port, String nextIp, int nextPort) {
        new Thread(() -> {
            new MetadataNode(nextIp, nextPort).start(port);
        }).start();
    }
}
