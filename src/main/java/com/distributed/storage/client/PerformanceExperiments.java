package com.distributed.storage.client;

import com.distributed.storage.metadata.MetadataNode;
import com.distributed.storage.network.TCPClient;
import com.distributed.storage.storage.StorageNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PerformanceExperiments {

    private static final String RESULTS_FILE = "results.csv";

    public static void main(String[] args) {
        System.out.println("=== STARTING PERFORMANCE EXPERIMENTS ===");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE))) {
            writer.println("Experiment,Variable,Value,AvgUploadLatency,AvgDownloadLatency,SuccessRate");
            
            runScalabilityExperiment(writer);
            runThroughputExperiment(writer);
            // runOverheadExperiment(writer); // Optional, might be complex to dynamic resize
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("=== EXPERIMENTS COMPLETED. Results saved to " + RESULTS_FILE + " ===");
        System.exit(0);
    }

    private static void runScalabilityExperiment(PrintWriter writer) throws Exception {
        System.out.println("\n[Experiment A] Scalability (Varying Clients)");
        int[] clientCounts = {1, 5, 10, 20, 50};
        int fileSize = 100 * 1024; // 100KB
        
        for (int clients : clientCounts) {
            System.out.println("  Running with " + clients + " clients...");
            startCluster(2); // 2 Storage Nodes
            Thread.sleep(2000); // Wait for cluster to stabilize
            
            ExperimentResult result = runWorkload(clients, fileSize);
            
            logResult(writer, "Scalability", "Clients", clients, result);
            stopCluster(2);
            Thread.sleep(2000); // Wait for ports to free
        }
    }

    private static void runThroughputExperiment(PrintWriter writer) throws Exception {
        System.out.println("\n[Experiment B] Throughput (Varying File Size)");
        int[] fileSizes = {10 * 1024, 100 * 1024, 1024 * 1024, 10 * 1024 * 1024}; // 10KB, 100KB, 1MB, 10MB
        int clients = 1;
        
        for (int size : fileSizes) {
            System.out.println("  Running with file size " + size + " bytes...");
            startCluster(2);
            Thread.sleep(2000);
            
            ExperimentResult result = runWorkload(clients, size);
            
            logResult(writer, "Throughput", "FileSize", size, result);
            stopCluster(2);
            Thread.sleep(2000);
        }
    }

    private static ExperimentResult runWorkload(int clientCount, int fileSize) throws Exception {
        List<String> storageNodes = new ArrayList<>();
        storageNodes.add("127.0.0.1:8001");
        storageNodes.add("127.0.0.1:8002");
        
        List<String> metadataNodes = new ArrayList<>();
        metadataNodes.add("127.0.0.1:9001");
        metadataNodes.add("127.0.0.1:9002");
        metadataNodes.add("127.0.0.1:9003");

        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        List<Long> uploadLatencies = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Long> downloadLatencies = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Boolean> successes = java.util.Collections.synchronizedList(new ArrayList<>());

        // Generate a dummy file of specific size
        byte[] data = new byte[fileSize];
        new java.util.Random().nextBytes(data);
        
        for (int i = 0; i < clientCount; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String fname = "perf_test_" + id + ".bin";
                    Files.write(Path.of(fname), data);
                    
                    Client c = new Client(storageNodes, metadataNodes);
                    
                    c.uploadFile(fname);
                    uploadLatencies.add(c.lastTotalUploadDuration);
                    
                    String outName = "perf_out_" + id + ".bin";
                    c.downloadFile(fname, outName);
                    downloadLatencies.add(c.lastTotalDownloadDuration);
                    
                    successes.add(true);
                    
                    // Cleanup
                    Files.deleteIfExists(Path.of(fname));
                    Files.deleteIfExists(Path.of(outName));
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    successes.add(false);
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);

        double avgUpload = uploadLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgDownload = downloadLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long successCount = successes.stream().filter(b -> b).count();
        double successRate = (double) successCount / clientCount * 100.0;

        return new ExperimentResult(avgUpload, avgDownload, successRate);
    }

    private static void logResult(PrintWriter writer, String experiment, String variable, int value, ExperimentResult result) {
        writer.printf("%s,%s,%d,%.2f,%.2f,%.2f%n", experiment, variable, value, result.avgUpload, result.avgDownload, result.successRate);
        writer.flush();
        System.out.printf("    -> Avg Upload: %.2f ms, Avg Download: %.2f ms, Success: %.2f%%%n", result.avgUpload, result.avgDownload, result.successRate);
    }

    // --- Cluster Management ---

    private static void startCluster(int storageCount) {
        for (int i = 1; i <= storageCount; i++) {
            int port = 8000 + i;
            new Thread(() -> new StorageNode().start(port)).start();
        }
        
        // Start Metadata Chain (Tail -> Mid -> Head)
        new Thread(() -> new MetadataNode("", -1).start(9003)).start();
        try { Thread.sleep(500); } catch (Exception e) {}
        new Thread(() -> new MetadataNode("127.0.0.1", 9003).start(9002)).start();
        try { Thread.sleep(500); } catch (Exception e) {}
        new Thread(() -> new MetadataNode("127.0.0.1", 9002).start(9001)).start();
    }

    private static void stopCluster(int storageCount) {
        for (int i = 1; i <= storageCount; i++) {
            killNode(8000 + i);
        }
        killNode(9001);
        killNode(9002);
        killNode(9003);
    }

    private static void killNode(int port) {
        TCPClient client = new TCPClient();
        if (client.connect("127.0.0.1", port)) {
            client.sendMessage("DIE");
            client.close();
        }
    }

    static class ExperimentResult {
        double avgUpload;
        double avgDownload;
        double successRate;

        public ExperimentResult(double u, double d, double s) {
            this.avgUpload = u;
            this.avgDownload = d;
            this.successRate = s;
        }
    }
}
