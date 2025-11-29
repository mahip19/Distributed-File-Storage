#include "TCPSocket.h"

#include <iostream>
#include <cstring>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>

using namespace std;

TCPClient::TCPClient() : fd_(-1), connected_(false) {}

TCPClient::~TCPClient()
{
    if (connected_)
    {
        Close();
    }
}

bool TCPClient::Connect(const string &ip, int port)
{
    fd_ = socket(AF_INET, SOCK_STREAM, 0);
    if (fd_ < 0)
    {
        cerr << "Error: Failed to create socket" << endl;
        return false;
    }

    sockaddr_in server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);

    if (inet_pton(AF_INET, ip.c_str(), &server_addr.sin_addr) <= 0)
    {
        cerr << "Erro: Invalid ip address" << endl;
        close(fd_);
        return false;
    }

    // connect
    if (connect(fd_, (sockaddr *)&server_addr, sizeof(server_addr)) < 0)
    {
        cerr << "Error: Conections failed" << endl;
        close(fd_);
        return false;
    }

    connected_ = true;

    return connected_;
}

bool TCPClient::SendAll(const char *data, size_t size)
{
    size_t total_sent = 0;
    while (total_sent < size)
    {
        ssize_t sent = send(fd_, data + total_sent, size - total_sent, 0);
        if (sent <= 0)
        {
            return false;
        }
        total_sent += sent;
    }
    return true;
}

bool TCPClient::RecvAll(char *buffer, size_t size)
{
    size_t total_recv = 0;
    while (total_recv < size)
    {
        ssize_t received = recv(fd_, buffer + total_recv, size - total_recv, 0);
        if (received <= 0)
        {
            return false;
        }
        total_recv += received;
    }
    return true;
}

bool TCPClient::SendMessage(const string &message)
{

    // send 4 byte length prefix
    uint32_t len = htonl(message.size());
    if (!SendAll((char *)&len, sizeof(len)))
    {
        return false;
    }

    // sends actual message
    return SendAll(message.data(), message.size());
}

string TCPClient::RecvMessage()
{
    uint32_t len_net;
    if (!RecvAll((char *)&len_net, sizeof(len_net)))
    {
        return "";
    }

    uint32_t len = ntohl(len_net);

    // receive actual messagte
    string message(len, '\0');
    if (!RecvAll(&message[0], len))
        return "";

    return message;
}

void TCPClient::Close()
{
    if (connected_)
    {
        shutdown(fd_, SHUT_RDWR);
        close(fd_);
        connected_ = false;
        fd_ = -1;
    }
}

// server

TCPServer::TCPServer() : listen_fd_(-1), running_(false) {}

TCPServer::~TCPServer()
{
    if (running_)
        Stop();
}

bool TCPServer::Start(int port)
{
    listen_fd_ = socket(AF_INET, SOCK_STREAM, 0);

    if (listen_fd_ < 0)
    {
        cerr << "Error: failed to create socket" << endl;
        return false;
    }

    // allow port reuse
    int opt = 1;
    setsockopt(listen_fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    // setup addr
    sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    // bind
    if (bind(listen_fd_, (sockaddr *)&addr, sizeof(addr)) < 0)
    {
        cerr << "Error: bind failed" << endl;
        close(listen_fd_);
        return false;
    }

    if (listen(listen_fd_, 10) < 0)
    {
        cerr << "error: listen failed" << endl;
        close(listen_fd_);
        return false;
    }

    running_ = true;
    return true;
}

int TCPServer::AcceptClient()
{
    sockaddr_in client_addr;
    socklen_t client_len = sizeof(client_addr);

    int client_fd = accept(listen_fd_, (sockaddr *)&client_addr, &client_len);
    if (client_fd < 0)
    {
        cerr << "Error: accept failed" << endl;
        return -1;
    }

    return client_fd;
}

bool TCPServer::SendAll(int fd, const char *data, size_t size)
{
    size_t total_sent = 0;
    while (total_sent < size)
    {
        ssize_t sent = send(fd, data + total_sent, size - total_sent, 0);
        if (sent <= 0)
        {
            return false;
        }
        total_sent += sent;
    }
    return true;
}

bool TCPServer::RecvAll(int fd, char *buffer, size_t size)
{
    size_t total_recv = 0;
    while (total_recv < size)
    {
        ssize_t received = recv(fd, buffer + total_recv, size - total_recv, 0);
        if (received <= 0)
        {
            return false;
        }
        total_recv += received;
    }
    return true;
}

bool TCPServer::SendMessage(int client_fd, const string &message)
{
    // Send 4-byte length prefix
    uint32_t len = htonl(message.size());
    if (!SendAll(client_fd, (char *)&len, sizeof(len)))
    {
        return false;
    }

    // Send actual message
    return SendAll(client_fd, message.data(), message.size());
}

string TCPServer::RecvMessage(int client_fd)
{
    // Receive 4-byte length prefix
    uint32_t len_net;
    if (!RecvAll(client_fd, (char *)&len_net, sizeof(len_net)))
    {
        return "";
    }
    uint32_t len = ntohl(len_net);

    // Receive actual message
    string message(len, '\0');
    if (!RecvAll(client_fd, &message[0], len))
    {
        return "";
    }

    return message;
}

void TCPServer::CloseClient(int client_fd)
{
    shutdown(client_fd, SHUT_RDWR);
    close(client_fd);
}

void TCPServer::Stop()
{
    if (running_)
    {
        close(listen_fd_);
        running_ = false;
        listen_fd_ = -1;
    }
}