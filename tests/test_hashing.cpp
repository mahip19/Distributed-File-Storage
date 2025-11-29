#include <iostream>

#include "FileUtils.h"
#include "HashUtils.h"

using namespace std;

int main()
{
    cout << "=== Test Hashing ===" << endl;

    // Create chunks from a test file
    vector<Chunk> chunks = SplitFileIntoChunks("data/small.txt");
    cout << "Chunks loaded: " << chunks.size() << endl;

    // Hash all chunks
    HashAllChunks(chunks);

    // Print chunk hashes
    for (const Chunk &chunk : chunks)
    {
        cout << "Chunk " << chunk.index << " hash: " << chunk.hash << endl;
    }

    // Collect hashes and compute root hash
    vector<string> chunk_hashes;
    for (const Chunk &chunk : chunks)
    {
        chunk_hashes.push_back(chunk.hash);
    }

    string root_hash = ComputeRootHash(chunk_hashes);
    cout << "Root hash (CID): " << root_hash << endl;

    return 0;
}