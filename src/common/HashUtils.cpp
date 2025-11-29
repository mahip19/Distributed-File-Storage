#include "HashUtils.h"

#include <iomanip>
#include <sstream>
#include <openssl/sha.h>

using namespace std;

string ComputeSHA256(const vector<char> &data)
{
    // This array will hold the raw hash
    unsigned char hash[SHA256_DIGEST_LENGTH]; // 32 bytes
    SHA256(reinterpret_cast<const unsigned char *>(data.data()), data.size(), hash);
    // Converts char* to unsigned char*

    /**
     *  Raw hash (32 bytes): [0x2c, 0xf2, 0x4d, 0xba, ...]

        After conversion (64 chars): "2cf24dba..."
     */
    stringstream ss;
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++)
    {
        ss << hex << setw(2) << setfill('0') << (int)hash[i];
    }

    return ss.str();
}

void HashChunk(Chunk &chunk)
{
    chunk.hash = ComputeSHA256(chunk.data);
}

void HashAllChunks(vector<Chunk> &chunks)
{
    for (Chunk &chunk : chunks)
    {
        HashChunk(chunk);
    }
}

string ComputeRootHash(const vector<string> &chunk_hashes)
{
    // Concatenate all chunk hashes
    string combined;
    for (const string &hash : chunk_hashes)
    {
        combined += hash;
    }

    // Hash the combined string
    vector<char> data(combined.begin(), combined.end());
    return ComputeSHA256(data);
}