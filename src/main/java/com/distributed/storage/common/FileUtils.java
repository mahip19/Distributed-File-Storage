package com.distributed.storage.common;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    public static final int CHUNK_SIZE = 1048576; // 1MB

    public static List<Chunk> splitFileIntoChunks(String filepath) {
        List<Chunk> chunks = new ArrayList<>();
        File file = new File(filepath);

        if (!file.exists()) {
            System.err.println("Error, file cannot be opened " + filepath);
            return chunks;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int index = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    Chunk chunk = new Chunk();
                    chunk.index = index++;
                    chunk.size = bytesRead;
                    // Create exact sized array
                    chunk.data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk.data, 0, bytesRead);
                    chunks.add(chunk);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chunks;
    }

    public static boolean reconstructFile(List<Chunk> chunks, String outputPath) {
        if (chunks == null || chunks.isEmpty()) {
            System.err.println("Empty chunks. cant reconstruct");
            return false;
        }

        List<Chunk> sortedChunks = new ArrayList<>(chunks);
        sortedChunks.sort(Comparator.comparingInt(a -> a.index));

        // check for missing chunks
        for (int i = 0; i < sortedChunks.size(); i++) {
            if (sortedChunks.get(i).index != i) {
                System.err.println("Error: missing chunk " + i);
                return false;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (Chunk chunk : sortedChunks) {
                fos.write(chunk.data);
            }
        } catch (IOException e) {
            System.err.println("Error: file could not be created " + outputPath);
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
