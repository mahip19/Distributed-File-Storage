package com.distributed.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class HashUtils {

    public static String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void hashChunk(Chunk chunk) {
        // Only hash the valid data, not the entire buffer if it's larger than size
        byte[] validData = new byte[chunk.size];
        System.arraycopy(chunk.data, 0, validData, 0, chunk.size);
        chunk.hash = computeSHA256(validData);
    }

    public static void hashAllChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            hashChunk(chunk);
        }
    }

    public static String computeRootHash(List<String> chunkHashes) {
        StringBuilder combined = new StringBuilder();
        for (String hash : chunkHashes) {
            combined.append(hash);
        }
        return computeSHA256(combined.toString().getBytes());
    }
}
