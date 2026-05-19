//
// Created by USS_Shenzhou on 2026/4/9.
//

#ifndef SUBSPACE_BASECONNECTION_H
#define SUBSPACE_BASECONNECTION_H

#include <memory>
#include <vector>
#include <asio.hpp>
#include <spdlog/spdlog.h>

#include "../util/Util.h"
#include "../util/CryptHelper.h"
#include "../util/FriendlyByteBuf.h"
#include "../manager/TokenManager.h"
#include "../manager/ShortIdManager.h"

namespace subspace {
    class BaseConnection : public std::enable_shared_from_this<BaseConnection> {
    public :
        explicit BaseConnection(
            const CryptFunc& encryptor,
            const CryptFunc& decryptor
            ) :
            encryptor(encryptor), decryptor(decryptor) {
        }

        virtual ~BaseConnection() = default;

        virtual void start() = 0;
        virtual void disconnect();

        virtual void send(const ByteArray& data) {
        }

        void setOnDisconnect(const std::function<void()>& runnable) {
            onDisconnect = runnable;
        }

    protected:
        CryptFunc encryptor;
        /**
         * @throw DecryptException
         */
        CryptFunc decryptor;
        std::function<void()> onDisconnect;

        void asyncRead(asio::ip::tcp::socket& socket, byte* out, int length, std::function<void()> doRead);
        void asyncRead(asio::ip::tcp::socket& socket, ByteArray& out, std::function<void()> doRead);

        void handleRaw(const ByteArray& encrypted);
        virtual const ByteArray& getToken() = 0;
        virtual void handle(FriendlyByteBuf& decrypted) = 0;
        virtual std::string getRemoteAddress() = 0;
    };
} // subspace

#endif //SUBSPACE_BASECONNECTION_H
