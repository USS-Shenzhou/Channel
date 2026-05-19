//
// Created by USS_Shenzhou on 2026/5/17.
//

#ifndef SUBSPACE_CLIENTTCPCONNECTION_H
#define SUBSPACE_CLIENTTCPCONNECTION_H


#include <deque>

#include "BaseConnection.h"
#include "../manager/RelayManager.h"

namespace subspace {
    class ClientTcpConnection : public BaseConnection {
    public:
        explicit ClientTcpConnection(asio::ip::tcp::socket socket);

        void start() override;
        void disconnect() override;
        void send(const ByteArray& data) override;

        UUID getPlayerUuid() const { return playerUuid; }

    protected:
        void waitAndReadPacketLengthHeader();
        void readPacketLengthHeader();
        void extractContent(int length);

        const ByteArray& getToken() override;
        void handle(FriendlyByteBuf& buf) override;

        std::string getRemoteAddress() override {
            return remoteAddress;
        }

        asio::ip::tcp::socket socket;
        byte headerBuf = 0;
        int headerBufValue = 0;
        int headerBufOffsetBit = 0;
        ByteArray content;

    private:
        asio::steady_timer handshakeTimer;
        bool waitingHandshake = true;
        UUID playerUuid{};
        asio::strand<asio::any_io_executor> strand;
        std::deque<std::shared_ptr<ByteArray>> writeQueue;
        std::string remoteAddress = "<NOT READY>";

        void handshake(FriendlyByteBuf& buf);
        void voiceFromClient(FriendlyByteBuf& buf);
    };
} // subspace


#endif //SUBSPACE_CLIENTTCPCONNECTION_H
