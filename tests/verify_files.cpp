// verify_files.cpp
// Compares two files by computing and comparing their CIDs

#include <iostream>
#include <string>
#include "FileUtils.h"
#include "HashUtils.h"

using namespace std;

string computeCID(const string &filepath)
{
    // Chunk the file
    vector<Chunk> chunks = SplitFileIntoChunks(filepath);
    if (chunks.empty())
    {
        return "";
    }

    // Hash all chunks
    HashAllChunks(chunks);

    // Collect chunk hashes
    vector<string> chunkHashes;
    for (const Chunk &chunk : chunks)
    {
        chunkHashes.push_back(chunk.hash);
    }

    // Compute root hash (CID)
    return ComputeRootHash(chunkHashes);
}

int main(int argc, char *argv[])
{
    if (argc != 3)
    {
        cerr << "Usage: " << argv[0] << " <original_file> <reconstructed_file>" << endl;
        return 1;
    }

    string originalPath = argv[1];
    string reconstructedPath = argv[2];

    cout << "Computing CID for original file..." << endl;
    string originalCID = computeCID(originalPath);
    if (originalCID.empty())
    {
        cerr << "Error: Could not process original file" << endl;
        return 1;
    }

    cout << "Computing CID for reconstructed file..." << endl;
    string reconstructedCID = computeCID(reconstructedPath);
    if (reconstructedCID.empty())
    {
        cerr << "Error: Could not process reconstructed file" << endl;
        return 1;
    }

    cout << "\n--- Results ---" << endl;
    cout << "Original CID:      " << originalCID << endl;
    cout << "Reconstructed CID: " << reconstructedCID << endl;

    if (originalCID == reconstructedCID)
    {
        cout << "\nVERIFIED: Files are identical" << endl;
        return 0;
    }
    else
    {
        cout << "\nMISMATCH: Files differ" << endl;
        return 1;
    }
}