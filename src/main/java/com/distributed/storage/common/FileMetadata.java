package com.distributed.storage.common;

import java.util.List;

public class FileMetadata {
    public String filename;
    public String rootHash; // CID
    public long fileSize;
    public int chunkSize; // 1MB
    public int totalChunks;
    public List<String> chunkHashes; // ordered list
}
