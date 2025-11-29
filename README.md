# Distributed File Storage (Java Port)

This is a Java port of the Distributed File Storage system.

## Project Structure

```
Distributed-File-Storage-Java/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── distributed/
│                   └── storage/
│                       ├── Chunk.java
│                       ├── ConsistentHash.java
│                       ├── FileMetadata.java
│                       ├── FileUtils.java
│                       ├── HashUtils.java
│                       ├── TCPClient.java
│                       ├── TCPServer.java
│                       └── VerifyFiles.java
```

## Build & Run

**Compile:**

```bash
javac -d out src/main/java/com/distributed/storage/*.java
```

**Run VerifyFiles:**

```bash
java -cp out com.distributed.storage.VerifyFiles <original_file> <reconstructed_file>
```
