#ifndef __HASH_UTILS_H__
#define __HASH_UTILS_H__

#include <string>
#include <vector>

#include "Chunk.h"

using namespace std;

// Compute SHA-256 hash of raw data
string ComputeSHA256(const vector<char> &data);

// Hash a single chunk, stores result in chunk.hash
void HashChunk(Chunk &chunk);

// Hash all chunks in a vector
void HashAllChunks(vector<Chunk> &chunks);

// Compute root hash (CID) from ordered chunk hashes
string ComputeRootHash(const vector<string> &chunk_hashes);

#endif // __HASH_UTILS_H__