//
// Created by USS_Shenzhou on 2026/4/9.
//

#ifndef SUBSPACE_SERVERCONNECTION_H
#define SUBSPACE_SERVERCONNECTION_H

#include "BaseConnection.h"

#include "../manager/RelayManager.h"


namespace subspace {
    class ServerConnection : public BaseConnection {
    public:
        explicit ServerConnection(asio::ip::tcp::socket socket);

        void start() override;
        void disconnect() override;

    protected:
        void waitAndReadPacketLengthHeader();
        void extractContent(int length);

        const ByteArray& getToken() override;
        void handle(FriendlyByteBuf& decrypted) override;

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
        std::string remoteAddress;

        void serverInit(FriendlyByteBuf& buf);
        void playerLogIn(FriendlyByteBuf& buf);
        void playerLogOut(FriendlyByteBuf& buf);
        void routeUpdate(FriendlyByteBuf& buf);

        void readPacketLengthHeader();
    };
} // subspace

#endif //SUBSPACE_SERVERCONNECTION_H
