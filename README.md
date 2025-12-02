# Distributed File Storage System

A scalable, fault-tolerant distributed file storage system implemented in Java. This project demonstrates core distributed computing concepts such as **Consistent Hashing** for data distribution and **Chain Replication** for strong metadata consistency.

## Project Goals

The primary goal of this project was to build a distributed system that ensures:
*   **Scalability**: Evenly distribute load across storage nodes using Consistent Hashing.
*   **Consistency**: Guarantee linearizable consistency for metadata using Chain Replication.
*   **Fault Tolerance**: Ensure data remains accessible even if a storage node fails (Replication Factor = 2).
*   **Performance**: Handle concurrent client requests efficiently using multi-threading.

## Architecture

The system is organized into three distinct layers:

### 1. Client Layer
*   **Function**: Acts as the entry point for users.
*   **Logic**: Splits files into 1MB chunks, computes SHA-256 Content IDs (CIDs), and orchestrates uploads/downloads. It uses the DHT to locate primary storage nodes and communicates with the Head of the metadata chain for updates.

### 2. Metadata Layer (Chain Replication)
*   **Topology**: A chain of 3 nodes: `Head -> Mid -> Tail`.
*   **Consistency**: Implements strong consistency. Writes are propagated down the chain and only acknowledged after reaching the Tail.
*   **Read Path**: Reads are served exclusively by the Tail to ensure the latest committed state is observed.

### 3. Storage Layer (DHT Ring)
*   **Partitioning**: Nodes are arranged on a consistent hash ring.
*   **Replication**: Each chunk is stored on its primary node and replicated to the next `k-1` nodes (Successor List) for fault tolerance.

## Key Features

*   **Consistent Hashing (DHT)**: Minimizes data movement during node additions/removals.
*   **Chain Replication**: Ensures strong consistency for file metadata.
*   **Fault Tolerance**:
    *   **Storage**: Automatic failover to replicas if a storage node goes down.
    *   **Metadata**: Client handles failover if the Head node becomes unresponsive.
*   **Concurrency**: Server nodes use `ExecutorService` (CachedThreadPool) to handle multiple concurrent connections.
*   **Integrity**: Verifies file integrity using SHA-256 hashing upon download.

## Performance Evaluation

The system was evaluated for **Scalability** and **Throughput**:

*   **Scalability**: Latency remains low (< 50ms) for up to 10 concurrent clients. As expected, latency increases linearly with more clients due to resource contention on a single test machine.
*   **Throughput**: The system efficiently handles larger files (1MB - 10MB), saturating available network bandwidth as the fixed overhead of metadata updates becomes negligible compared to data transfer time.

> See `graph_scalability.png` and `graph_throughput.png` for detailed performance visualizations.

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

## Setup & Usage

### Prerequisites
*   Java 17 or higher
*   Make (optional, for easier build)
*   Python 3 (for generating performance graphs)

### Building the Project
Using Make:
```bash
make
```

Or manually:
```bash
mkdir -p out
javac -d out src/main/java/com/distributed/storage/**/*.java
```

### Running System Tests
To run the automated fault tolerance and concurrency tests:
```bash
make test
# OR
java -cp out com.distributed.storage.client.SystemTests
```

### Generating Performance Report
To generate the performance graphs (`graph_scalability.png`, `graph_throughput.png`):
```bash
python plot_results.py
```
*Note: Requires `pandas` (`pip install pandas`).*

## Running on Khoury Linux Cluster

To deploy and run on the Khoury Linux cluster (e.g., `linux-079.khoury.northeastern.edu`):

1.  **Transfer Code**:
    ```bash
    scp -r Distributed-File-Storage-Java/ <your_username>@linux-079.khoury.northeastern.edu:~/
    ```

2.  **SSH & Compile**:
    ```bash
    ssh <your_username>@linux-079.khoury.northeastern.edu
    cd Distributed-File-Storage-Java
    make
    ```

3.  **Run Evaluation**:
    ```bash
    make test
    ```

4.  **Run Performance Experiments**:
    ```bash
    java -cp out com.distributed.storage.client.PerformanceExperiments
    ```

## Future Work

*   **Persistent Storage**: Integrate RocksDB or LevelDB to replace the current in-memory storage, allowing data to survive process restarts.
*   **Dynamic Configuration**: Improve client configuration to avoid hardcoded IP addresses and ports.

## Contributors

*   **Samyak Shah**
*   **Mahip Parekh**

---
*CS 6650: Building Scalable Distributed Systems | December 2025*
