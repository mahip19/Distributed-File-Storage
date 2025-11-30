#ifndef CONSISTENTHASH_H
#define CONSISTENTHASH_H

#include <string>
#include <map>
#include <vector>
#include <functional>

class ConsistentHash
{
private:
    // The ring: maps position (hash value) → node address
    std::map<size_t, std::string> ring;

    // Hash function to map strings to positions on the ring
    size_t HashKey(const std::string &key) const;

public:
    // Add a node to the ring
    void AddNode(const std::string &nodeAddress);

    // Remove a node from the ring
    void RemoveNode(const std::string &nodeAddress);

    // Get the node responsible for a given key (CID)
    std::string GetNodeForKey(const std::string &key) const;

    std::vector<std::string> GetNodesForKey(const std::string &key, int replicaCount) const;

    // Get all nodes in the ring
    std::vector<std::string> GetAllNodes() const;

    // Get number of nodes
    size_t GetNodeCount() const;

    // Check if a node exists in the ring
    bool HasNode(const std::string &nodeAddress) const;

    // Debug: print the ring structure
    void PrintRing() const;
};

#endif