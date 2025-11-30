package com.distributed.storage.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TCPServer {
    private ServerSocket serverSocket;
    private boolean running;
    
    private Map<Integer, SocketContext> clients = new ConcurrentHashMap<>();
    private int nextClientId = 1;

    private static class SocketContext {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;

        SocketContext(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }
    }

    public TCPServer() {
        this.running = false;
    }

    public boolean start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            return true;
        } catch (IOException e) {
            System.err.println("Error: failed to create socket or bind");
            return false;
        }
    }

    public int acceptClient() {
        if (!running) return -1;
        try {
            Socket clientSocket = serverSocket.accept();
            int clientId = nextClientId++;
            clients.put(clientId, new SocketContext(clientSocket));
            return clientId;
        } catch (IOException e) {
            System.err.println("Error: accept failed");
            return -1;
        }
    }

    public boolean sendData(int clientId, byte[] data) {
        SocketContext ctx = clients.get(clientId);
        if (ctx == null) return false;
        try {
            ctx.out.writeInt(data.length);
            ctx.out.write(data);
            ctx.out.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] recvData(int clientId) {
        SocketContext ctx = clients.get(clientId);
        if (ctx == null) return null;
        try {
            int length = ctx.in.readInt();
            byte[] data = new byte[length];
            ctx.in.readFully(data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean sendMessage(int clientId, String message) {
        return sendData(clientId, message.getBytes(StandardCharsets.UTF_8));
    }

    public String recvMessage(int clientId) {
        byte[] data = recvData(clientId);
        if (data == null) return "";
        return new String(data, StandardCharsets.UTF_8);
    }

    public void closeClient(int clientId) {
        SocketContext ctx = clients.remove(clientId);
        if (ctx != null) {
            try {
                ctx.socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void stop() {
        if (running) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
            running = false;
            // Close all clients
            for (Integer id : clients.keySet()) {
                closeClient(id);
            }
        }
    }
}
