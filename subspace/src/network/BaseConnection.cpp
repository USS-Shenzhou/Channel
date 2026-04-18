//
// Created by USS_Shenzhou on 2026/4/9.
//

#include "BaseConnection.h"

namespace subspace {
    void BaseConnection::asyncRead(asio::ip::tcp::socket& socket, byte* out, int length, std::function<void()> doRead) {
        auto thiz = shared_from_this();
        asio::async_read(socket, asio::buffer(out, length), [this, thiz, doRead](auto errorCode, int readBytes)-> void {
            if (errorCode) {
                spdlog::warn("Something went wrong: {}", errorCode.message());
                disconnect();
                return;
            }
            doRead();
        });
    }

    void BaseConnection::asyncRead(asio::ip::tcp::socket& socket, ByteArray& out, std::function<void()> doRead) {
        auto thiz = shared_from_this();
        asio::async_read(socket, asio::buffer(out), [this, thiz, doRead](auto errorCode, int readBytes)-> void {
            if (errorCode) {
                spdlog::warn("Something went wrong: {}", errorCode.message());
                disconnect();
                return;
            }
            doRead();
        });
    }

    void BaseConnection::handleRaw(const ByteArray& encrypted) {
        try {
            auto decrypted = decryptor(encrypted, getToken());
            auto buf = FriendlyByteBuf(decrypted);
            handle(buf);
        } catch (std::exception& e) {
            spdlog::warn("Something went wrong: {}, disconnecting.", e.what());
            disconnect();
        }
    }

    void BaseConnection::disconnect() {
        if (onDisconnect) {
            onDisconnect();
        }
    }
}
