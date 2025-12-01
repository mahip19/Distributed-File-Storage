package com.distributed.storage.common;

public class NodeInfo {
    private int id;
    private String host;
    private int port;

    public NodeInfo(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return "NodeInfo{id=" + id + ", host='" + host + "', port=" + port + "}";
    }
}
