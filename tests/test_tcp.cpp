// test_tcp.cpp
// Tests basic TCP client-server communication

#include <iostream>
#include <thread>
#include <chrono>
#include "TCPSocket.h"

using namespace std;

void runServer(int port)
{
    TCPServer server;

    if (!server.Start(port))
    {
        cerr << "Server failed to start" << endl;
        return;
    }
    cout << "[Server] Started on port " << port << endl;

    // Wait for a client
    int client_fd = server.AcceptClient();
    if (client_fd < 0)
    {
        cerr << "[Server] Failed to accept client" << endl;
        return;
    }
    cout << "[Server] Client connected" << endl;

    // Receive message from client
    string msg = server.RecvMessage(client_fd);
    cout << "[Server] Received: " << msg << endl;

    // Send reply
    string reply = "Hello from server!";
    server.SendMessage(client_fd, reply);
    cout << "[Server] Sent reply" << endl;

    // Cleanup
    server.CloseClient(client_fd);
    server.Stop();
    cout << "[Server] Stopped" << endl;
}

void runClient(int port)
{
    // Small delay to let server start
    this_thread::sleep_for(chrono::milliseconds(100));

    TCPClient client;

    if (!client.Connect("127.0.0.1", port))
    {
        cerr << "[Client] Failed to connect" << endl;
        return;
    }
    cout << "[Client] Connected to server" << endl;

    // Send message
    string msg = "Hello from client!";
    client.SendMessage(msg);
    cout << "[Client] Sent: " << msg << endl;

    // Receive reply
    string reply = client.RecvMessage();
    cout << "[Client] Received: " << reply << endl;

    // Cleanup
    client.Close();
    cout << "[Client] Disconnected" << endl;
}

int main()
{
    int port = 8080;

    cout << "=== TCP Test ===" << endl;

    // Run server in a separate thread
    thread serverThread(runServer, port);

    // Run client in main thread
    runClient(port);

    // Wait for server to finish
    serverThread.join();

    cout << "\n=== Test Complete ===" << endl;
    return 0;
}