#include "ConsistentHash.h"
#include <iostream>

using namespace std;

size_t ConsistentHash::HashKey(const string &key) const
{
    // Using std::hash for simplicity
    // In production, you might want a better distributed hash like MurmurHash
    hash<string> hasher;
    return hasher(key);
}

void ConsistentHash::AddNode(const string &nodeAddress)
{
    size_t pos = HashKey(nodeAddress);
    ring[pos] = nodeAddress;
}

void ConsistentHash::RemoveNode(const string &nodeAddress)
{
    size_t pos = HashKey(nodeAddress);
    ring.erase(pos);
}

string ConsistentHash::GetNodeForKey(const string &key) const
{
    if (ring.empty())
    {
        return "";
    }

    size_t pos = HashKey(key);

    // Find the first node with position >= key's position
    // This is "walking clockwise" on the ring
    auto it = ring.lower_bound(pos);

    // If we've gone past the end, wrap around to the first node
    if (it == ring.end())
    {
        it = ring.begin();
    }

    return it->second;
}

vector<string> ConsistentHash::GetAllNodes() const
{
    vector<string> nodes;
    for (const auto &pair : ring)
    {
        nodes.push_back(pair.second);
    }
    return nodes;
}

size_t ConsistentHash::GetNodeCount() const
{
    return ring.size();
}

bool ConsistentHash::HasNode(const string &nodeAddress) const
{
    size_t pos = HashKey(nodeAddress);
    return ring.find(pos) != ring.end();
}

void ConsistentHash::PrintRing() const
{
    cout << "=== Consistent Hash Ring ===" << endl;
    cout << "Nodes: " << ring.size() << endl;

    for (const auto &pair : ring)
    {
        cout << "  Position " << pair.first << " → " << pair.second << endl;
    }

    cout << "=============================" << endl;
}