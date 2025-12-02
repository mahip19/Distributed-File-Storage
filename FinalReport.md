# Final Project Report: Distributed File Storage System

**Team Members:**
*   Samyak Shah
*   Mahip Parekh

---

## 1. Detailed Description of the Project

This project implements a distributed file storage system designed to handle file uploads and downloads across a cluster of nodes. The system splits files into fixed-size chunks (1MB) and distributes them across multiple storage nodes to ensure scalability and fault tolerance.

The core architectural components are:
*   **Client**: Handles file chunking, hashing (SHA-256), and orchestration of uploads/downloads.
*   **Storage Nodes**: Responsible for storing the actual file chunks. They act as a key-value store.
*   **Metadata Nodes**: Manage the mapping between filenames and their constituent chunks.
*   **Consistent Hashing (DHT)**: Used to determine which storage node is responsible for a given chunk, minimizing data movement during node additions/removals.
*   **Chain Replication**: Implemented for the metadata layer to ensure strong consistency. Writes propagate from Head -> Mid -> Tail, while reads are served by the Tail.

## 2. Project Goal

The primary goal of this project was to build a scalable, fault-tolerant distributed system that demonstrates core distributed computing concepts. Specifically, we aimed to:
1.  Implement **Consistent Hashing** to evenly distribute load across storage nodes.
2.  Implement **Chain Replication** to guarantee linearizable consistency for metadata.
3.  Ensure **Fault Tolerance** so that data remains accessible even if a storage node fails (via replication factor k=2).
4.  Achieve high **Performance** using multi-threading (Thread Pools) to handle concurrent client requests.

## 3. Software Design and Implementation

### 3.1 Architecture
The system is divided into three distinct layers:

*   **Client Layer**:
    *   **Function**: Entry point for users. Splits files into 1MB chunks and computes a SHA-256 Content ID (CID) for each.
    *   **Logic**: Uses the DHT to find the primary storage node for each chunk and its replicas. Uploads metadata to the Head of the metadata chain.

*   **Metadata Layer (Chain Replication)**:
    *   **Topology**: A chain of 3 nodes: Head -> Mid -> Tail.
    *   **Consistency**: Strong consistency. A write is only acknowledged after it has propagated to the Tail.
    *   **Read Path**: Clients read only from the Tail to ensure they see the latest committed state.

*   **Storage Layer (DHT Ring)**:
    *   **Partitioning**: Nodes are arranged on a consistent hash ring.
    *   **Replication**: Each chunk is stored on its primary node and the next $k-1$ nodes in the ring (Successor List).

### 3.2 Key Implementation Details
*   **Language**: Java 17+
*   **Communication**: TCP Sockets with `DataOutputStream`/`DataInputStream` for custom length-prefixed messaging.
*   **Concurrency**: `ExecutorService` (CachedThreadPool) used in all server nodes to handle multiple concurrent connections.
*   **Storage**: In-memory `ConcurrentHashMap` used to simulate storage nodes (as per project constraints to demonstrate volatile storage handling).

## 4. Achievements and Non-Achievements

### Achieved
*   [x] **Core Functionality**: Successful upload and download of files of varying sizes.
*   [x] **Fault Tolerance**: System survives the failure of a storage node. Data is seamlessly retrieved from replicas.
*   [x] **Consistency**: Metadata updates are linearizable due to Chain Replication.
*   [x] **Performance**: System handles multiple concurrent clients (tested up to 50) with reasonable latency.
*   [x] **Automated Testing**: Comprehensive system tests (`SystemTests.java`) verify correctness and performance.

### Not Achieved / Future Work
*   [ ] **Dynamic Node Recovery**: While the system handles node failure, automatically re-joining a failed node and backfilling data (hinted handoff) is partially implemented but not fully automated.
*   [ ] **Persistent Storage**: Currently uses in-memory storage. Integrating RocksDB or LevelDB would provide persistence across process restarts.

## 5. Evaluation of the System

We evaluated the system using two primary metrics: **Scalability** and **Throughput**.

### 5.1 Scalability (Latency vs. Concurrent Clients)
We measured the average upload and download latency as the number of concurrent clients increased from 1 to 50.
*   **Observation**: Latency remains low (< 50ms) for up to 10 clients. As load increases to 50 clients, latency increases linearly, which is expected due to resource contention on the test machine.
*   **Graph**: See `graph_scalability.png` in the attached report.

### 5.2 Throughput (Latency vs. File Size)
We measured latency for files ranging from 10KB to 10MB.
*   **Observation**: The system shows efficient handling of larger files. The overhead per chunk is minimal.
*   **Graph**: See `graph_throughput.png` in the attached report.

## 6. Achievements and Changes from Plan

*   **Pivot to Chain Replication**: As noted in the project plan, we initially considered a blockchain-based approach but pivoted to Chain Replication to focus on strong consistency and availability, which better aligned with the course goals.
*   **Simplification of Consensus**: We chose Chain Replication over Raft/Paxos for the metadata layer due to its simplicity and high read throughput (reads served by Tail).

## 7. What You Have Learned

*(Please fill this section with your personal learnings. Example: "We learned the complexities of debugging distributed systems, specifically race conditions in the replication logic...")*

## 8. Setup, Run, and Test Instructions

### Prerequisites
*   Java 17 or higher
*   Make (optional, for easier build)
*   Python 3 (for generating graphs)

### Building the Project
```bash
make
# OR manually:
mkdir -p out
javac -d out src/main/java/com/distributed/storage/**/*.java
```

### Running System Tests (Local Evaluation)
This runs the automated fault tolerance and concurrency tests:
```bash
make test
# OR
java -cp out com.distributed.storage.client.SystemTests
```

### Generating Performance Report
```bash
python plot_results.py
```

## 9. Instructions for Khoury Linux Cluster

To run this on the Khoury Linux cluster (e.g., `linux-079.khoury.northeastern.edu`), follow these detailed steps:

1.  **Transfer Code**:
    ```bash
    scp -r Distributed-File-Storage/ <your_username>@linux-079.khoury.northeastern.edu:~/
    ```

2.  **SSH into the machine**:
    ```bash
    ssh <your_username>@linux-079.khoury.northeastern.edu
    cd Distributed-File-Storage
    ```

3.  **Compile**:
    ```bash
    make
    ```

4.  **Run Evaluation**:
    Since the cluster machines share a filesystem (NFS), you can run the `SystemTests` directly, which spawns processes on the local machine (simulating a cluster).
    ```bash
    make test
    ```
    *Note: If you intend to run nodes on *different* physical machines, you would need to modify `nodes.conf` to use the actual IP addresses of the machines (e.g., `linux-080`, `linux-081`) and start the `StorageNode` and `MetadataNode` classes manually on those machines.*

## 10. Individual Contributions

### Samyak Shah
*   *(Please fill in details, e.g., "Implemented the DHT logic and Storage Node server...")*

### Mahip Parekh
*   *(Please fill in details, e.g., "Implemented the Client CLI, Chain Replication logic, and System Tests...")*
