//
// Created by USS_Shenzhou on 2026/5/17.
//

#include "ClientTcpConnection.h"

namespace subspace {
    namespace {
        CryptFunc getEncoder() {
            switch (*getConfig().securityLevel) {
                case SecurityLevel::MID:
                    return CryptHelper::makeAesCtrEncoder();
                case SecurityLevel::HIGH:
                    return CryptHelper::makeAesGcmEncoder();
                default:
                    return CryptHelper::NO_ENCODE_DECODE;
            }
        }

        CryptFunc getHandShakeDecoder() {
            switch (*getConfig().securityLevel) {
                case SecurityLevel::NONE:
                    return CryptHelper::NO_ENCODE_DECODE;
                default:
                    return CryptHelper::AES_GCM_ONCE_DECODE;
            }
        }

        CryptFunc getDecoder() {
            switch (*getConfig().securityLevel) {
                case SecurityLevel::MID:
                    return CryptHelper::AES_CTR_DECODE;
                case SecurityLevel::HIGH:
                    return CryptHelper::AES_GCM_DECODE;
                default:
                    return CryptHelper::NO_ENCODE_DECODE;
            }
        }
    }

    ClientTcpConnection::ClientTcpConnection(asio::ip::tcp::socket socket) :
        BaseConnection(getEncoder(), getHandShakeDecoder()),
        socket(std::move(socket)),
        handshakeTimer(this->socket.get_executor()),
        strand(asio::make_strand(this->socket.get_executor())) {
    }

    void ClientTcpConnection::start() {
        socket.set_option(asio::ip::tcp::no_delay(true));
        remoteAddress = socket.remote_endpoint().address().to_string() + ":" + std::to_string(socket.remote_endpoint().port());
        spdlog::info("Client connecting from {}", getRemoteAddress());
        handshakeTimer.expires_after(std::chrono::seconds(5));
        handshakeTimer.async_wait([thiz = std::dynamic_pointer_cast<ClientTcpConnection>(shared_from_this())](const std::error_code& ec) {
            if (ec) {
                return;
            }
            if (thiz->waitingHandshake) {
                spdlog::warn("Client handshake timeout from {}", thiz->getRemoteAddress());
                thiz->disconnect();
            }
        });
        waitAndReadPacketLengthHeader();
    }

    void ClientTcpConnection::waitAndReadPacketLengthHeader() {
        headerBufValue = 0;
        headerBufOffsetBit = 0;
        readPacketLengthHeader();
    }

    void ClientTcpConnection::readPacketLengthHeader() {
        asyncRead(socket, &headerBuf, 1, [this]() {
            headerBufValue |= (headerBuf & 0x7F) << headerBufOffsetBit;
            if ((headerBuf & 0x80) == 0) {
                if (headerBufValue < 0) {
                    spdlog::warn("Invalid packet length header: {}", headerBufValue);
                    disconnect();
                    return;
                }
                extractContent(headerBufValue);
            } else {
                headerBufOffsetBit += 7;
                if (headerBufOffsetBit >= 35) {
                    spdlog::warn("VarInt too big from client {}", getRemoteAddress());
                    disconnect();
                    return;
                }
                readPacketLengthHeader();
            }
        });
    }

    void ClientTcpConnection::extractContent(int length) {
        content.resize(length);
        asyncRead(socket, content, [this]() {
            if (waitingHandshake) {
                FriendlyByteBuf buf(content);
                playerUuid = buf.readUUID();
                if (!TokenManager::contains(playerUuid)) {
                    spdlog::warn("Unknown player UUID from {}, rejecting.", getRemoteAddress());
                    disconnect();
                    return;
                }
            }
            handleRaw(content);
            waitAndReadPacketLengthHeader();
        });
    }

    void ClientTcpConnection::disconnect() {
        if (!socket.is_open()) { return; }
        spdlog::info("Disconnecting from client {}", getRemoteAddress());
        std::error_code ec;
        socket.close(ec);
        RelayManager::disconnect(playerUuid, this);
        BaseConnection::disconnect();
    }

    const ByteArray& ClientTcpConnection::getToken() {
        return TokenManager::get(playerUuid);
    }

    void ClientTcpConnection::handle(FriendlyByteBuf& buf) {
        if (waitingHandshake) {
            handshake(buf);
        } else {
            voiceFromClient(buf);
        }
    }

    void ClientTcpConnection::handshake(FriendlyByteBuf& buf) {
        waitingHandshake = false;
        handshakeTimer.cancel();
        spdlog::info("Client {} connected, uuid={}, shortId={}.", getRemoteAddress(), playerUuid.toString(), ShortIdManager::get(playerUuid));
        decryptor = getDecoder();
        RelayManager::registerConnection(playerUuid, std::dynamic_pointer_cast<ClientTcpConnection>(shared_from_this()));
    }

    void ClientTcpConnection::voiceFromClient(FriendlyByteBuf& buf) {
        auto opus = buf.readByteArray();
        RelayManager::relay(playerUuid, opus);
    }

    void ClientTcpConnection::send(const ByteArray& data) {
        if (waitingHandshake) {
            return;
        }

        FriendlyByteBuf buf;
        buf.writeByteArray(encryptor(data, getToken()));
        auto bytes = buf.bytes();

        auto thiz = std::dynamic_pointer_cast<ClientTcpConnection>(shared_from_this());

        static std::function<void(std::shared_ptr<ClientTcpConnection> thiz)> doWrite = [](std::shared_ptr<ClientTcpConnection> thiz)-> void {
            asio::async_write(
                thiz->socket,
                asio::buffer(*thiz->writeQueue.front()),
                asio::bind_executor(thiz->strand, [thiz](const asio::error_code& ec, std::size_t) {
                    if (ec) {
                        spdlog::warn("Send to {} failed: {}", thiz->getRemoteAddress(), ec.message());
                        thiz->disconnect();
                        return;
                    }
                    thiz->writeQueue.pop_front();
                    if (!thiz->writeQueue.empty()) {
                        doWrite(thiz);
                    }
                })
                );
        };

        asio::post(strand, [thiz, bytes]() -> void {
            const bool idle = thiz->writeQueue.empty();
            thiz->writeQueue.push_back(bytes);
            if (idle) {
                doWrite(thiz);
            }
        });
    }
} // subspace
