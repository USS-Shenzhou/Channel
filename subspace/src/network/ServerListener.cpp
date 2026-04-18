//
// Created by USS_Shenzhou on 2026/4/9.
//

#include "ServerListener.h"

namespace subspace {
    ServerListener::ServerListener(asio::io_context& context) :
        listener(context, asio::ip::tcp::endpoint(asio::ip::tcp::v4(), getConfig().serverPort)) {
        spdlog::info("Listening on {}", getConfig().serverPort);
        wait();
    }

    void ServerListener::wait() {
        listener.async_accept([this](auto errorCode, auto socket) -> void {
            if (errorCode) {
                spdlog::error("Something went wrong when establishing connection with {} : {}", socket.remote_endpoint().address().to_string(), errorCode.message());
            } else {
                auto connection = std::make_shared<ServerConnection>(std::move(socket));
                connection->setOnDisconnect([this, c = connection.get()]()-> void {
                    this->remove(c);
                });
                connections.push_back(connection);
                connection->start();
            }
            wait();
        });
    }

    void ServerListener::remove(ServerConnection* connection) {
        std::lock_guard lock(mutex);
        std::erase_if(connections, [connection](const auto& c) {
            return c.get() == connection;
        });
    }
} // subspace
