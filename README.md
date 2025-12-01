# Distributed File Storage (Java)

A distributed file storage system implementing **Consistent Hashing** for data distribution and **Chain Replication** for metadata consistency.

## Features
*   **Scalability**: Uses **Consistent Hashing** (DHT) to distribute file chunks across storage nodes.
*   **Consistency**: Uses **Chain Replication** (Head -> Mid -> Tail) for strong consistency in metadata.
*   **Performance**: Implements **Thread Pools** in all server nodes to handle concurrent client requests efficiently.
*   **Fault Tolerance**:
    *   **Storage**: Chunks are replicated (k=2). If a node fails, data is retrieved from its replica.
    *   **Metadata**: Client automatically fails over to the next node if the Head fails.
*   **Integrity**: Automatic SHA-256 verification of downloaded files.

## Architecture

### Components

1.  **Client**: 
    *   Splits files into 1MB chunks.
    *   Computes SHA-256 Content IDs (CIDs).
    *   Uploads chunks to **Storage Nodes** (Replication Factor = 2).
    *   Uploads metadata to **Metadata Ring** (Head).
2.  **Storage Node**:
    *   Stores raw chunk data.
    *   Key-Value store interface (`STORE`, `GET`).
    *   Uses `ExecutorService` (Thread Pool) for concurrency.
3.  **Metadata Node**:
    *   Stores file metadata (filename -> chunks list).
    *   Implements **Chain Replication** (Head -> Mid -> Tail).
    *   **Writes**: Sent to Head, propagated to Tail.
    *   **Reads**: Served by Tail (Strong Consistency).

### Data Flow

*   **Upload**: Client -> Chunk Hashing -> DHT Lookup -> Upload to Storage Nodes -> Upload Metadata to Head.
*   **Download**: Client -> Get Metadata from Tail -> DHT Lookup -> Download Chunks from Storage Nodes -> Reconstruct.

## Project Structure

```
src/main/java/com/distributed/storage/
├── client/         # Client application & System Tests
├── common/         # Shared utilities (Hashing, File I/O)
├── dht/            # Consistent Hashing implementation
├── metadata/       # Metadata Node (Chain Replication)
├── network/        # TCP Networking primitives
├── storage/        # Storage Node implementation
```

## Build & Run (Linux/Mac)

This project uses a **Makefile** for easy compilation and testing.

### 1. Compile
```bash
make
```

### 2. Run Experiments (Evaluation)
To verify the system's functionality, fault tolerance, and performance, run the automated system tests:
```bash
make test
```
**What this does:**
*   Starts a local cluster (2 Storage Nodes, 3 Metadata Nodes).
*   **Experiment A (Fault Tolerance)**: Uploads a file, kills a Storage Node, and verifies the file can still be downloaded from the replica.
*   **Experiment B (Concurrency)**: Spawns 10 concurrent clients to upload/download files simultaneously.

### 3. Clean
```bash
make clean
```

## Manual Build & Run (No Makefile)

If `make` is not available, or if you prefer running commands manually, follow these steps:

### 1. Compile
Run this command from the project root:
```bash
mkdir -p out
javac -d out src/main/java/com/distributed/storage/common/*.java \
             src/main/java/com/distributed/storage/dht/*.java \
             src/main/java/com/distributed/storage/network/*.java \
             src/main/java/com/distributed/storage/storage/*.java \
             src/main/java/com/distributed/storage/metadata/*.java \
             src/main/java/com/distributed/storage/client/*.java
```

### 2. Run Experiments (Evaluation)
```bash
java -cp out com.distributed.storage.client.SystemTests
```

### 3. Run Manual Client (Interactive)
```bash
java -cp out com.distributed.storage.client.Client
```
