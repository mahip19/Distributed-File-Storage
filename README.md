# Distributed File Storage (Java)

A distributed file storage system implementing **Consistent Hashing** for data distribution and **Chain Replication** for metadata consistency.

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
├── client/         # Client application & verification
├── common/         # Shared utilities (Hashing, File I/O)
├── dht/            # Consistent Hashing implementation
├── metadata/       # Metadata Node (Chain Replication)
├── network/        # TCP Networking primitives
└── storage/        # Storage Node implementation
```

## Build & Run

**Compile:**

```bash
javac -d out src/main/java/com/distributed/storage/common/*.java \
             src/main/java/com/distributed/storage/dht/*.java \
             src/main/java/com/distributed/storage/network/*.java \
             src/main/java/com/distributed/storage/storage/*.java \
             src/main/java/com/distributed/storage/metadata/*.java \
             src/main/java/com/distributed/storage/client/*.java
```

**Run Full System Test:**

This runs a self-contained test that spawns 3 Storage Nodes and 3 Metadata Nodes locally, uploads a file, and downloads it.

```bash
java -cp out com.distributed.storage.client.Client
```

## Manual Deployment

**Start Storage Nodes:**
```bash
java -cp out com.distributed.storage.storage.StorageNode 8001
java -cp out com.distributed.storage.storage.StorageNode 8002
...
```

**Start Metadata Nodes (Chain: 9001 -> 9002 -> 9003):**
```bash
# Tail
java -cp out com.distributed.storage.metadata.MetadataNode 9003
# Mid
java -cp out com.distributed.storage.metadata.MetadataNode 9002 127.0.0.1 9003
# Head
java -cp out com.distributed.storage.metadata.MetadataNode 9001 127.0.0.1 9002
```
