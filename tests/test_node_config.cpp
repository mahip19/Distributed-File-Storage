#include "NodeConfig.h"
#include <fstream>
#include <sstream>
#include <iostream>

using namespace std;

string NodeInfo::GetAddress() const
{
    return host + ":" + to_string(port);
}

bool NodeConfig::LoadFromFile(const string &configPath)
{
    ifstream file(configPath);

    if (!file.is_open())
    {
        cerr << "Error: Cannot open config file: " << configPath << endl;
        return false;
    }

    nodes.clear();
    nodeMap.clear();

    string line;
    int lineNum = 0;

    while (getline(file, line))
    {
        lineNum++;

        // Skip empty lines and comments
        if (line.empty() || line[0] == '#')
        {
            continue;
        }

        istringstream iss(line);
        NodeInfo node;

        if (!(iss >> node.id >> node.host >> node.port))
        {
            cerr << "Warning: Invalid format at line " << lineNum << ": " << line << endl;
            continue;
        }

        nodes.push_back(node);
        nodeMap[node.id] = node;
    }

    file.close();

    if (nodes.empty())
    {
        cerr << "Error: No valid nodes found in config file" << endl;
        return false;
    }

    return true;
}

void NodeConfig::SetMyNodeId(const string &nodeId)
{
    myNodeId = nodeId;
}

NodeInfo NodeConfig::GetMyNode() const
{
    if (nodeMap.find(myNodeId) != nodeMap.end())
    {
        return nodeMap.at(myNodeId);
    }

    cerr << "Error: My node ID '" << myNodeId << "' not found in config" << endl;
    return NodeInfo();
}

NodeInfo NodeConfig::GetNode(const string &nodeId) const
{
    if (nodeMap.find(nodeId) != nodeMap.end())
    {
        return nodeMap.at(nodeId);
    }

    cerr << "Error: Node ID '" << nodeId << "' not found in config" << endl;
    return NodeInfo();
}

vector<NodeInfo> NodeConfig::GetAllNodes() const
{
    return nodes;
}

vector<NodeInfo> NodeConfig::GetPeerNodes() const
{
    vector<NodeInfo> peers;

    for (const NodeInfo &node : nodes)
    {
        if (node.id != myNodeId)
        {
            peers.push_back(node);
        }
    }

    return peers;
}

size_t NodeConfig::GetNodeCount() const
{
    return nodes.size();
}

bool NodeConfig::HasNode(const string &nodeId) const
{
    return nodeMap.find(nodeId) != nodeMap.end();
}

void NodeConfig::PrintNodes() const
{
    cout << "=== Node Configuration ===" << endl;
    cout << "Total nodes: " << nodes.size() << endl;
    cout << "My node ID: " << (myNodeId.empty() ? "(not set)" : myNodeId) << endl;
    cout << endl;

    for (const NodeInfo &node : nodes)
    {
        cout << "  " << node.id << ": " << node.GetAddress();
        if (node.id == myNodeId)
        {
            cout << " (me)";
        }
        cout << endl;
    }

    cout << "===========================" << endl;
}