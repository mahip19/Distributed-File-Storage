#include <iostream>
#include <cstdio>
#include "FileUtils.h"
#include "HashUtils.h"

using namespace std;

int main(int argc, char *argv[])
{
    if (argc < 2 || argc > 3)
    {
        cerr << "Usage: " << argv[0] << " <filepath> [--keep]" << endl;
        cerr << "Example: " << argv[0] << " data/test_image.png" << endl;
        cerr << "         " << argv[0] << " data/test_image.png --keep" << endl;
        return 1;
    }

    string inputPath = argv[1];
    bool keepFile = (argc == 3 && string(argv[2]) == "--keep");
    string outputPath = inputPath + ".reconstructed";

    // Step 1: Chunk the file
    cout << "[1/4] Chunking " << inputPath << "..." << endl;
    vector<Chunk> chunks = SplitFileIntoChunks(inputPath);

    if (chunks.empty())
    {
        cerr << "Error: Could not chunk file (empty or not found)" << endl;
        return 1;
    }
    cout << "      Created " << chunks.size() << " chunk(s)" << endl;

    // Step 2: Hash all chunks and compute original CID
    cout << "[2/4] Computing original CID..." << endl;
    HashAllChunks(chunks);

    vector<string> chunkHashes;
    for (const Chunk &chunk : chunks)
    {
        chunkHashes.push_back(chunk.hash);
    }
    string originalCID = ComputeRootHash(chunkHashes);
    cout << "      CID: " << originalCID << endl;

    // Step 3: Reconstruct the file
    cout << "[3/4] Reconstructing to " << outputPath << "..." << endl;
    if (!ReconstructFile(chunks, outputPath))
    {
        cerr << "Error: Failed to reconstruct file" << endl;
        return 1;
    }

    // Step 4: Chunk reconstructed file and compute its CID
    cout << "[4/4] Verifying reconstructed file..." << endl;
    vector<Chunk> reconstructedChunks = SplitFileIntoChunks(outputPath);
    HashAllChunks(reconstructedChunks);

    vector<string> reconstructedHashes;
    for (const Chunk &chunk : reconstructedChunks)
    {
        reconstructedHashes.push_back(chunk.hash);
    }
    string reconstructedCID = ComputeRootHash(reconstructedHashes);
    cout << "      CID: " << reconstructedCID << endl;

    // Result
    cout << endl;
    cout << "================================" << endl;

    if (originalCID == reconstructedCID)
    {
        cout << "Result: PASS" << endl;
        cout << "File integrity verified!" << endl;

        if (keepFile)
        {
            cout << "Reconstructed file kept at: " << outputPath << endl;
        }
        else
        {
            remove(outputPath.c_str());
            cout << "(Cleaned up " << outputPath << ")" << endl;
        }
        return 0;
    }
    else
    {
        cout << "Result: FAIL" << endl;
        cout << "CID mismatch! Keeping " << outputPath << " for inspection." << endl;
        return 1;
    }
}