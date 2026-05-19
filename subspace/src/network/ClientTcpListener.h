//
// Created by USS_Shenzhou on 2026/5/17.
//

#ifndef SUBSPACE_CLIENTTCPLISTENER_H
#define SUBSPACE_CLIENTTCPLISTENER_H

#include <mutex>
#include <asio.hpp>
#include "ClientTcpConnection.h"
#include "../Config.h"

namespace subspace {
    class ClientTcpListener {
    public:
        ClientTcpListener(asio::io_context& context);
        void remove(ClientTcpConnection* connection);

    private:
        void wait();
        asio::ip::tcp::acceptor listener;
        std::mutex mutex;
        std::vector<std::shared_ptr<ClientTcpConnection>> connections;
    };
} // subspace


#endif //SUBSPACE_CLIENTTCPLISTENER_H
