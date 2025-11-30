package com.distributed.storage.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean connected;

    public TCPClient() {
        this.connected = false;
    }

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Error: Connections failed");
            return false;
        }
    }

    public boolean sendData(byte[] data) {
        if (!connected) return false;
        try {
            out.writeInt(data.length); // 4 bytes length
            out.write(data);
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] recvData() {
        if (!connected) return null;
        try {
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean sendMessage(String message) {
        return sendData(message.getBytes(StandardCharsets.UTF_8));
    }

    public String recvMessage() {
        byte[] data = recvData();
        if (data == null) return "";
        return new String(data, StandardCharsets.UTF_8);
    }

    public void close() {
        if (connected) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
