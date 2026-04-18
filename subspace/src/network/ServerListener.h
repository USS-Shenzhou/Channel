//
// Created by USS_Shenzhou on 2026/4/9.
//

#ifndef SUBSPACE_SERVERLISTENER_H
#define SUBSPACE_SERVERLISTENER_H

#include <asio.hpp>
#include "ServerConnection.h"
#include "../Config.h"

namespace subspace {
    class ServerListener {
    public:
        ServerListener(asio::io_context &context);
        void remove(ServerConnection *connection);

    private:
        void wait();
        asio::ip::tcp::acceptor listener;
        std::mutex mutex;
        std::vector<std::shared_ptr<ServerConnection>> connections;
    };
} // subspace

#endif //SUBSPACE_SERVERLISTENER_H
