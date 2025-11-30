package com.distributed.storage.dht;

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
        Map.Entry<Integer, String> entry = ring.ceilingEntry(pos);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    // Get k distinct nodes for replication (walking clockwise)
    public List<String> getNodesForKey(String key, int k) {
        List<String> nodes = new ArrayList<>();
        if (ring.isEmpty()) return nodes;

        int pos = hashKey(key);
        Map.Entry<Integer, String> entry = ring.ceilingEntry(pos);
        
        // Start from the found position (or wrap around)
        if (entry == null) {
            entry = ring.firstEntry();
        }

        // We need to iterate through the ring to find k distinct values
        // Since multiple positions might map to the same physical node (if we had vnodes),
        // but here we map pos -> address directly.
        // However, we should handle the case where we have fewer nodes than k.
        
        // Get all entries in a list to iterate easily with wrapping
        List<String> allValues = new ArrayList<>(ring.values());
        // This list is sorted by position because ring is a TreeMap
        
        // But wait, ring.values() returns values in key order.
        // We need to start from 'entry' and go clockwise.
        
        // Let's use the keys to iterate
        List<Integer> keys = new ArrayList<>(ring.keySet());
        int startIndex = keys.indexOf(entry.getKey());
        
        for (int i = 0; i < keys.size() && nodes.size() < k; i++) {
            int currentIndex = (startIndex + i) % keys.size();
            String node = ring.get(keys.get(currentIndex));
            if (!nodes.contains(node)) {
                nodes.add(node);
            }
        }
        
        return nodes;
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
