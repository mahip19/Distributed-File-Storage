#include "FileUtils.h"

#include <fstream>
#include <iostream>
#include <algorithm>

using namespace std;

vector<Chunk> SplitFileIntoChunks(const string &filepath)
{
    vector<Chunk> chunks;

    // open file in binary mode
    ifstream file(filepath, ios::binary);

    if (!file.is_open())
    {
        cerr << "Error, file cannot be opened " << filepath << endl;
        return chunks;
    }

    int index = 0;

    while (true)
    {
        // create buffer of 1mb
        vector<char> buffer(CHUNK_SIZE);

        file.read(buffer.data(), CHUNK_SIZE);

        size_t bytes_read = file.gcount();

        // if we read something, make chunks
        if (bytes_read > 0)
        {
            Chunk chunk;
            chunk.index = index++;
            chunk.size = bytes_read;
            chunk.data = vector<char>(buffer.begin(), buffer.begin() + bytes_read);
            // todo: fill chunk.hash later using hashing utiliy
            chunks.push_back(chunk);
        }
        if (file.eof())
            break;
    }
    file.close();
    return chunks;
}

bool ReconstructFile(const vector<Chunk> &chunks, const string &output_path)
{
    if (chunks.empty())
    {
        cerr << "Empty chunks. cant reconstruct" << endl;
        return false;
    }

    vector<Chunk> sorted_chunks = chunks;
    sort(sorted_chunks.begin(), sorted_chunks.end(),
         [](const Chunk &a, const Chunk &b)
         {
             return a.index < b.index;
         });

    // check for misisng chunks.
    for (size_t i = 0; i < sorted_chunks.size(); i++)
    {
        if (sorted_chunks[i].index != (int)i)
        {
            cerr << "Error: missing chunk " << i << endl;
            return false;
        }
    }

    // open file from output path in write mode
    ofstream file(output_path, ios::binary);
    if (!file.is_open())
    {
        cerr << "Erorr: file could not be created " << output_path << endl;
        return false;
    }

    // write each chunks data
    for (const Chunk &chunk : sorted_chunks)
    {
        file.write(chunk.data.data(), chunk.size);
    }

    file.close();
    return true;
}