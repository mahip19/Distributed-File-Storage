package com.distributed.storage;

import java.util.ArrayList;
import java.util.List;

public class VerifyFiles {

    public static String computeCID(String filepath) {
        // Chunk the file
        List<Chunk> chunks = FileUtils.splitFileIntoChunks(filepath);
        if (chunks.isEmpty()) {
            return "";
        }

        // Hash all chunks
        HashUtils.hashAllChunks(chunks);

        // Collect chunk hashes
        List<String> chunkHashes = new ArrayList<>();
        for (Chunk chunk : chunks) {
            chunkHashes.add(chunk.hash);
        }

        // Compute root hash (CID)
        return HashUtils.computeRootHash(chunkHashes);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java VerifyFiles <original_file> <reconstructed_file>");
            System.exit(1);
        }

        String originalPath = args[0];
        String reconstructedPath = args[1];

        System.out.println("Computing CID for original file...");
        String originalCID = computeCID(originalPath);
        if (originalCID.isEmpty()) {
            System.err.println("Error: Could not process original file");
            System.exit(1);
        }

        System.out.println("Computing CID for reconstructed file...");
        String reconstructedCID = computeCID(reconstructedPath);
        if (reconstructedCID.isEmpty()) {
            System.err.println("Error: Could not process reconstructed file");
            System.exit(1);
        }

        System.out.println("\n--- Results ---");
        System.out.println("Original CID:      " + originalCID);
        System.out.println("Reconstructed CID: " + reconstructedCID);

        if (originalCID.equals(reconstructedCID)) {
            System.out.println("\nVERIFIED: Files are identical");
            System.exit(0);
        } else {
            System.out.println("\nMISMATCH: Files differ");
            System.exit(1);
        }
    }
}
