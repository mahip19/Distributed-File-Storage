package com.distributed.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConsistentHash {
    // The ring: maps position (hash value) -> node address
    private TreeMap<Integer, String> ring = new TreeMap<>();

    // Hash function to map strings to positions on the ring
    private int hashKey(String key) {
        // Using String.hashCode for simplicity
        // In production, you might want a better distributed hash like MurmurHash
        return key.hashCode();
    }

    // Add a node to the ring
    public void addNode(String nodeAddress) {
        int pos = hashKey(nodeAddress);
        ring.put(pos, nodeAddress);
    }

    // Remove a node from the ring
    public void removeNode(String nodeAddress) {
        int pos = hashKey(nodeAddress);
        ring.remove(pos);
    }

    // Get the node responsible for a given key (CID)
    public String getNodeForKey(String key) {
        if (ring.isEmpty()) {
            return "";
        }

        int pos = hashKey(key);

        // Find the first node with position >= key's position
        // This is "walking clockwise" on the ring
        Map.Entry<Integer, String> entry = ring.ceilingEntry(pos);

        // If we've gone past the end, wrap around to the first node
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    // Get all nodes in the ring
    public List<String> getAllNodes() {
        return new ArrayList<>(ring.values());
    }

    // Get number of nodes
    public int getNodeCount() {
        return ring.size();
    }

    // Check if a node exists in the ring
    public boolean hasNode(String nodeAddress) {
        int pos = hashKey(nodeAddress);
        return ring.containsKey(pos);
    }

    // Debug: print the ring structure
    public void printRing() {
        System.out.println("=== Consistent Hash Ring ===");
        System.out.println("Nodes: " + ring.size());

        for (Map.Entry<Integer, String> entry : ring.entrySet()) {
            System.out.println("  Position " + entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println("=============================");
    }
}
