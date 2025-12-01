package com.distributed.storage.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NodeConfig {
    private List<NodeInfo> nodes;
    private int myNodeId;

    public NodeConfig(String configFilePath, int myNodeId) {
        this.nodes = new ArrayList<>();
        this.myNodeId = myNodeId;
        loadConfig(configFilePath);
    }

    // Constructor for client (no specific node ID)
    public NodeConfig(String configFilePath) {
        this.nodes = new ArrayList<>();
        this.myNodeId = -1;
        loadConfig(configFilePath);
    }

    private void loadConfig(String configFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length == 3) {
                    int id = Integer.parseInt(parts[0]);
                    String host = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    nodes.add(new NodeInfo(id, host, port));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public NodeInfo getMyNode() {
        for (NodeInfo node : nodes) {
            if (node.getId() == myNodeId) {
                return node;
            }
        }
        return null;
    }

    public List<NodeInfo> getAllNodes() {
        return new ArrayList<>(nodes);
    }

    public List<NodeInfo> getPeerNodes() {
        return nodes.stream()
                .filter(node -> node.getId() != myNodeId)
                .collect(Collectors.toList());
    }
    
    public NodeInfo getNodeById(int id) {
        for (NodeInfo node : nodes) {
            if (node.getId() == id) {
                return node;
            }
        }
        return null;
    }
}
