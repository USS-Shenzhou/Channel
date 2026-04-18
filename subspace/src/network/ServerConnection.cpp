//
// Created by USS_Shenzhou on 2026/4/9.
//

#include "ServerConnection.h"

namespace subspace {
    ServerConnection::ServerConnection(asio::ip::tcp::socket socket) :
        BaseConnection(CryptHelper::NULL_ENCODE_DECODE, CryptHelper::AES_GCM_DECODE), socket(std::move(socket)), handshakeTimer(this->socket.get_executor()) {
    }

    void ServerConnection::start() {
        spdlog::info("Minecraft Server connecting, from {}", getRemoteAddress());
        handshakeTimer.expires_from_now(std::chrono::seconds(3));
        handshakeTimer.async_wait([thiz = std::dynamic_pointer_cast<ServerConnection>(shared_from_this())](const std::error_code& ec) {
            if (ec) {
                return;
            }
            if (thiz->waitingHandshake) {
                spdlog::warn("Handshake timeout from {}", thiz->getRemoteAddress());
                thiz->disconnect();
            }
        });
        waitAndReadPacketLengthHeader();
    }


    void ServerConnection::waitAndReadPacketLengthHeader() {
        headerBufValue = 0;
        headerBufOffsetBit = 0;
        readPacketLengthHeader();
    }

    void ServerConnection::readPacketLengthHeader() {
        asyncRead(socket, &headerBuf, 1, [this]() -> void {
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
                    spdlog::warn("VarInt too big from {}", getRemoteAddress());
                    disconnect();
                    return;
                }
                readPacketLengthHeader();
            }
        });
    }

    void ServerConnection::disconnect() {
        if (!socket.is_open()) {
            return;
        }
        spdlog::info("Disconnecting from {}", getRemoteAddress());
        socket.close();
        BaseConnection::disconnect();
    }

    void ServerConnection::extractContent(int length) {
        content.resize(length);
        asyncRead(socket, content, [this]()-> void {
            handleRaw(content);
            waitAndReadPacketLengthHeader();
        });
    }

    const ByteArray& ServerConnection::getToken() {
        return TokenManager::getServerToken();
    }

    void ServerConnection::handle(FriendlyByteBuf& decrypted) {
        int id = decrypted.readVarInt();
        switch (id) {
            case 0:
                serverInit(decrypted);
                break;
            case 1:
                playerLogIn(decrypted);
                break;
            case 2:
                playerLogOut(decrypted);
                break;
            case 3:
                routeUpdate(decrypted);
                break;
            default:
                spdlog::warn("Unknown server packet id {} from {}.", id, getRemoteAddress());
        }
    }

    void ServerConnection::serverInit(FriendlyByteBuf& buf) {
        waitingHandshake = false;
        handshakeTimer.cancel();
        auto protocol = buf.readEnum<Protocol>();
        auto securityLevel = buf.readEnum<SecurityLevel>();
        auto& cfg = getConfig();
        if (ready() && (*cfg.protocol != protocol || *cfg.securityLevel != securityLevel)) {
            spdlog::warn("Server {} trying to use different protocol and security level: [{}, {}]. Refusing.",
                         getRemoteAddress(), static_cast<int>(protocol), static_cast<int>(securityLevel));
            disconnect();
            return;
        }
        cfg.protocol = protocol;
        cfg.securityLevel = securityLevel;
        spdlog::info("Connection from {} accepted.", getRemoteAddress());
        spdlog::info("Now using protocol {} and security level {}.", static_cast<int>(protocol), static_cast<int>(securityLevel));
    }

    void ServerConnection::playerLogIn(FriendlyByteBuf& buf) {
        auto token = buf.readByteArray();
        auto uuid = buf.readUUID();
        auto id = buf.readVarInt();
        TokenManager::put(uuid, token);
        ShortIdManager::put(uuid, id);
    }

    void ServerConnection::playerLogOut(FriendlyByteBuf& buf) {
        auto uuid = buf.readUUID();
        TokenManager::remove(uuid);
        ShortIdManager::remove(uuid);
        // Client needs to disconnect actively.
    }

    void ServerConnection::routeUpdate(FriendlyByteBuf& buf) {
        //TODO
    }
} // subspace
