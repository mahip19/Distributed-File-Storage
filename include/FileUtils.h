#ifndef __FILE_UTILS_H__
#define __FILE_UTILS_H__

#include <string>
#include <vector>

#include "Chunk.h"

const size_t CHUNK_SIZE = 1048576; // 1MB

// Splits a file into chunks, returns vector of Chunk structs
//  hash field will be empty—filled in later by hashing utility
std::vector<Chunk> SplitFileIntoChunks(const std::string &filepath);

// Reassembles chunks into a file
bool ReconstructFile(const std::vector<Chunk> &chunks, const std::string &output_path);

#endif // __FILE_UTILS_H__