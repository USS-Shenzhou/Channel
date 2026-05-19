//
// Created by USS_Shenzhou on 2026/5/17.
//

#include "ClientTcpListener.h"

namespace subspace {
    ClientTcpListener::ClientTcpListener(asio::io_context& context) :
        listener(context, asio::ip::tcp::endpoint(asio::ip::tcp::v4(), getConfig().clientPort)) {
        spdlog::info("Listening on {} for client", getConfig().clientPort);
        wait();
    }

    void ClientTcpListener::wait() {
        listener.async_accept([this](auto errorCode, auto socket) {
            if (errorCode) {
                spdlog::error("Something went wrong when establishing connection with {} : {}", socket.remote_endpoint().address().to_string(), errorCode.message());
            } else {
                auto connection = std::make_shared<ClientTcpConnection>(std::move(socket));
                connection->setOnDisconnect([this, c = connection.get()]() {
                    this->remove(c);
                });
                connections.push_back(connection);
                connection->start();
            }
            wait();
        });
    }

    void ClientTcpListener::remove(ClientTcpConnection* connection) {
        std::lock_guard lock(mutex);
        std::erase_if(connections, [connection](const auto& c) {
            return c.get() == connection;
        });
    }
} // subspace