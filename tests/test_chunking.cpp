#include <iostream>
#include <fstream>

#include "FileUtils.h"

using namespace std;

// Helper: create a test file of given size
void CreateTestFile(const string &path, size_t size)
{
    ofstream file(path, ios::binary);
    for (size_t i = 0; i < size; i++)
    {
        char c = 'A' + (i % 26); // repeating A-Z
        file.write(&c, 1);
    }
    file.close();
    cout << "Created test file: " << path << " (" << size << " bytes)" << endl;
}

int main()
{
    cout << "=== Test 1: Small file (500 bytes) ===" << endl;
    CreateTestFile("data/small.txt", 500);
    vector<Chunk> chunks1 = SplitFileIntoChunks("data/small.txt");
    cout << "Chunks created: " << chunks1.size() << endl;
    ReconstructFile(chunks1, "data/small_reconstructed.txt");
    cout << endl;

    cout << "=== Test 2: Exactly 1MB ===" << endl;
    CreateTestFile("data/exact1mb.txt", 1048576);
    vector<Chunk> chunks2 = SplitFileIntoChunks("data/exact1mb.txt");
    cout << "Chunks created: " << chunks2.size() << endl;
    ReconstructFile(chunks2, "data/exact1mb_reconstructed.txt");
    cout << endl;

    cout << "=== Test 3: 2.5MB file ===" << endl;
    CreateTestFile("data/medium.txt", 2621440);
    vector<Chunk> chunks3 = SplitFileIntoChunks("data/medium.txt");
    cout << "Chunks created: " << chunks3.size() << endl;
    ReconstructFile(chunks3, "data/medium_reconstructed.txt");
    cout << endl;

    cout << "=== Test 4: Empty file ===" << endl;
    CreateTestFile("data/empty.txt", 0);
    vector<Chunk> chunks4 = SplitFileIntoChunks("data/empty.txt");
    cout << "Chunks created: " << chunks4.size() << endl;
    cout << endl;

    cout << "Tests complete! Compare original vs reconstructed files." << endl;
    return 0;
}