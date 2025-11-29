#ifndef __FILE_METADATA_H__
#define __FILE_METADATA_H__

#include <string>
#include <vector>

struct FileMetadata
{
    std::string filename;
    std::string root_hash; // CID
    size_t file_size;
    size_t chunk_size; // 1MB
    int total_chunks;
    std::vector<std::string> chunk_hashes; // ordered list
};

#endif // __FILE_METADATA_H__