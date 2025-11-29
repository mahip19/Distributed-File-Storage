# Distributed File Storage

A peer-to-peer file storage system built in C++ with content-addressed chunks, SHA-256 hashing, and TCP-based node communication.

## Project Structure

```
Distributed_File_Storage/
├── include/           # Header files
│   ├── Chunk.h
│   ├── FileMetaData.h
│   ├── FileUtils.h
│   ├── HashUtils.h
│   └── TCPSocket.h
├── src/
│   ├── client/        # Client module (TODO)
│   ├── common/        # Shared utilities
│   │   ├── FileUtils.cpp
│   │   ├── HashUtils.cpp
│   │   └── TCPSocket.cpp
│   ├── dht/           # DHT module (TODO)
│   └── server/        # Storage node (TODO)
├── tests/
│   ├── test_chunking.cpp
│   ├── test_hashing.cpp
│   ├── test_tcp.cpp
│   └── verify_files.cpp
└── data/              # Test files
```

## Completed

- [x] File chunking (1MB chunks)
- [x] SHA-256 hashing (per chunk + root hash/CID)
- [x] File reconstruction from chunks
- [x] Verification utility (compare original vs reconstructed)
- [x] TCP client/server with length-prefixed messages

## TODO

- [ ] Storage Node (store/retrieve chunks)
- [ ] DHT (consistent hashing, node lookup)
- [ ] Metadata Coordinator (primary-backup replication)
- [ ] Client CLI (upload/download interface)

## Build & Run

**Compile tests:**

```bash
# Verification utility
g++ -o verify_files tests/verify_files.cpp src/common/FileUtils.cpp src/common/HashUtils.cpp -I include -lssl -lcrypto

# TCP test
g++ -o test_tcp tests/test_tcp.cpp src/common/TCPSocket.cpp -I include -pthread
```

**Run:**

```bash
./verify_files data/small.txt data/small_reconstructed.txt
./test_tcp
```

## Dependencies

- OpenSSL (for SHA-256)
- POSIX sockets (Linux)

## Team

- Samyak Shah
- Mahip Parekh
