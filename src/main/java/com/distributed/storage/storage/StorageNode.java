package com.distributed.storage.storage;

import com.distributed.storage.network.TCPServer;
import java.util.concurrent.ConcurrentHashMap;

public class StorageNode {
    private TCPServer server;
    private ConcurrentHashMap<String, byte[]> storage;
    private boolean running;

    public StorageNode() {
        server = new TCPServer();
        storage = new ConcurrentHashMap<>();
        running = false;
    }

    public void start(int port) {
        if (!server.start(port)) {
            System.err.println("Failed to start storage node on port " + port);
            return;
        }
        running = true;
        System.out.println("Storage Node started on port " + port);

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

            String[] parts = command.split(" ");
            String op = parts[0];

            if ("STORE".equals(op) && parts.length == 2) {
                String hash = parts[1];
                server.sendMessage(clientId, "READY");
                byte[] data = server.recvData(clientId);
                if (data != null) {
                    storage.put(hash, data);
                    server.sendMessage(clientId, "ACK");
                    System.out.println("Stored chunk: " + hash + " (" + data.length + " bytes)");
                } else {
                    break;
                }
            } else if ("GET".equals(op) && parts.length == 2) {
                String hash = parts[1];
                byte[] data = storage.get(hash);
                if (data != null) {
                    server.sendMessage(clientId, "FOUND");
                    server.sendData(clientId, data);
                    System.out.println("Served chunk: " + hash);
                } else {
                    server.sendMessage(clientId, "NOT_FOUND");
                }
            } else {
                server.sendMessage(clientId, "ERROR");
            }
        }
        server.closeClient(clientId);
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java StorageNode <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        new StorageNode().start(port);
    }
}
