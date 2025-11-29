#include <iostream>
#include <fstream>
#include <cstring>

#include "FileUtils.h"

using namespace std;

// Create a test file of given size
void CreateTestFile(const string &path, size_t size)
{
    ofstream file(path, ios::binary);
    for (size_t i = 0; i < size; i++)
    {
        char c = 'A' + (i % 26);
        file.write(&c, 1);
    }
    file.close();
    cout << "Created test file: " << path << " (" << size << " bytes)" << endl;
}

// Compare two files byte-by-byte
bool FilesAreIdentical(const string &file1, const string &file2)
{
    ifstream f1(file1, ios::binary | ios::ate);
    ifstream f2(file2, ios::binary | ios::ate);

    if (!f1.is_open() || !f2.is_open())
    {
        cerr << "Error: Could not open files for comparison" << endl;
        return false;
    }

    // Quick check: compare file sizes first
    if (f1.tellg() != f2.tellg())
    {
        return false;
    }

    // Reset to beginning
    f1.seekg(0);
    f2.seekg(0);

    // Compare in chunks (more efficient than byte-by-byte)
    const size_t bufferSize = 4096;
    vector<char> buffer1(bufferSize);
    vector<char> buffer2(bufferSize);

    while (f1 && f2)
    {
        f1.read(buffer1.data(), bufferSize);
        f2.read(buffer2.data(), bufferSize);

        size_t bytesRead1 = f1.gcount();
        size_t bytesRead2 = f2.gcount();

        if (bytesRead1 != bytesRead2)
        {
            return false;
        }

        if (memcmp(buffer1.data(), buffer2.data(), bytesRead1) != 0)
        {
            return false;
        }
    }

    return true;
}

// Run a single test: create, chunk, reconstruct, verify
bool RunTest(const string &testName, const string &basePath, size_t fileSize)
{
    cout << "=== " << testName << " ===" << endl;

    string originalPath = basePath + ".txt";
    string reconstructedPath = basePath + "_reconstructed.txt";

    CreateTestFile(originalPath, fileSize);

    vector<Chunk> chunks = SplitFileIntoChunks(originalPath);
    cout << "Chunks created: " << chunks.size() << endl;

    if (fileSize == 0)
    {
        // Special case: empty file should produce 0 chunks
        bool passed = (chunks.size() == 0);
        cout << "Result: " << (passed ? "PASS" : "FAIL") << endl
             << endl;
        return passed;
    }

    ReconstructFile(chunks, reconstructedPath);

    bool passed = FilesAreIdentical(originalPath, reconstructedPath);
    cout << "Result: " << (passed ? "PASS" : "FAIL") << endl
         << endl;

    return passed;
}

int main()
{
    int passed = 0;
    int total = 4;

    if (RunTest("Test 1: Small file (500 bytes)", "data/small", 500))
        passed++;

    if (RunTest("Test 2: Exactly 1MB", "data/exact1mb", 1048576))
        passed++;

    if (RunTest("Test 3: 2.5MB file", "data/medium", 2621440))
        passed++;

    if (RunTest("Test 4: Empty file", "data/empty", 0))
        passed++;

    cout << "================================" << endl;
    cout << "Tests passed: " << passed << "/" << total << endl;

    if (passed == total)
    {
        cout << "All tests PASSED!" << endl;
        return 0;
    }
    else
    {
        cout << "Some tests FAILED!" << endl;
        return 1;
    }
}