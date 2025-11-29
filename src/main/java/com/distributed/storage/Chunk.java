package com.distributed.storage;

public class Chunk {
    public int index;              // position in file (0, 1, 2...)
    public String hash;            // SHA-256 of data
    public byte[] data;            // raw bytes
    public int size;               // actual size of this chunk

    @Override
    public String toString() {
        return "Chunk{index=" + index + ", hash='" + hash + "', size=" + size + "}";
    }
}
