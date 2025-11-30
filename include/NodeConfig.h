#ifndef NODECONFIG_H
#define NODECONFIG_H

#include <string>
#include <vector>
#include <map>

struct NodeInfo
{
    std::string id;   // e.g., "node0"
    std::string host; // e.g., "linux-079.khoury.northeastern.edu"
    int port;         // e.g., 8001

    // Returns "host:port" format
    std::string GetAddress() const;
};

class NodeConfig
{
private:
    std::vector<NodeInfo> nodes;
    std::map<std::string, NodeInfo> nodeMap; // id → NodeInfo
    std::string myNodeId;

public:
    // Load nodes from config file
    bool LoadFromFile(const std::string &configPath);

    // Set which node this instance is
    void SetMyNodeId(const std::string &nodeId);

    // Get my own node info
    NodeInfo GetMyNode() const;

    // Get a specific node by ID
    NodeInfo GetNode(const std::string &nodeId) const;

    // Get all nodes
    std::vector<NodeInfo> GetAllNodes() const;

    // Get all nodes except myself
    std::vector<NodeInfo> GetPeerNodes() const;

    // Get number of nodes
    size_t GetNodeCount() const;

    // Check if a node ID exists
    bool HasNode(const std::string &nodeId) const;

    // Debug: print all nodes
    void PrintNodes() const;
};

#endif