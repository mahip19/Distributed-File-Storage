#ifndef __TCP_SOCKET_H__
#define __TCP_SOCKET_H__

#include <string>
#include <functional>

using namespace std;

class TCPClient
{
public:
    TCPClient();
    ~TCPClient();

    // connect to a server
    bool Connect(const string &ip, int port);

    // sends msg
    bool SendMessage(const string &message);

    // receives message
    string RecvMessage();

    void Close();

    bool IsConnected() const { return connected_; }

private:
    int fd_;
    bool connected_;
    // helpers
    bool SendAll(const char *data, size_t size);
    bool RecvAll(char *buffer, size_t size);
};

class TCPServer
{
public:
    TCPServer();
    ~TCPServer();

    // start listeninig on a port, returns true on success
    bool Start(int port);

    // accepts a single client connection, returns client socket fd
    int AcceptClient();

    // sends msg to specific client fd
    bool SendMessage(int client_fd, const string &message);

    // recv message from a sp client fd
    string RecvMessage(int client_fd);

    void CloseClient(int client_fd);

    void Stop();

private:
    int listen_fd_;
    bool running_;

    // helpers
    bool SendAll(int fd, const char *data, size_t size);
    bool RecvAll(int fd, char *buffer, size_t size);
};

#endif