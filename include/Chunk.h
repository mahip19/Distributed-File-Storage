#ifndef __CHUNK_H__
#define __CHUNK_H__

#include <string>
#include <vector>

struct Chunk
{
    int index;              // position in file (0, 1, 2...)
    std::string hash;       // SHA-256 of data
    std::vector<char> data; // raw bytes
    size_t size;            // actual size of this chunk
};

#endif // __CHUNK_H__