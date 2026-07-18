//
// Created by USS_Shenzhou on 2026/4/9.
//

#include "ServerConnection.h"

namespace subspace {
    ServerConnection::ServerConnection(asio::ip::tcp::socket socket) :
        BaseConnection(CryptHelper::NULL_ENCODE_DECODE, CryptHelper::AES_GCM_DECODE), socket(std::move(socket)), handshakeTimer(this->socket.get_executor()) {}

    void ServerConnection::start() {
        socket.set_option(asio::ip::tcp::no_delay(true));
        remoteAddress = socket.remote_endpoint().address().to_string() + ":" + std::to_string(socket.remote_endpoint().port());
        spdlog::info("Minecraft Server connecting, from {}", getRemoteAddress());
        handshakeTimer.expires_from_now(std::chrono::seconds(10));
        handshakeTimer.async_wait([thiz = std::dynamic_pointer_cast<ServerConnection>(shared_from_this())](const std::error_code& ec) {
            if (ec) {
                return;
            }
            if (thiz->waitingHandshake) {
                spdlog::warn("Server handshake timeout from {}", thiz->getRemoteAddress());
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
        spdlog::info("Disconnecting from server {}", getRemoteAddress());
        std::error_code ec;
        socket.close(ec);
        BaseConnection::disconnect();
    }

    void ServerConnection::extractContent(int length) {
        content.resize(length);
        asyncRead(socket, content, [this]() -> void {
            handleRaw(content);
            waitAndReadPacketLengthHeader();
        });
    }

    const ByteArray& ServerConnection::getToken() { return TokenManager::getServerToken(); }

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

    void initClientListening();

    void ServerConnection::serverInit(FriendlyByteBuf& buf) {
        waitingHandshake = false;
        handshakeTimer.cancel();
        auto protocol = buf.readEnum<Protocol>();
        auto securityLevel = buf.readEnum<SecurityLevel>();
        auto& cfg = getConfig();
        if (ready() && (*cfg.protocol != protocol || *cfg.securityLevel != securityLevel)) {
            spdlog::warn("Server {} trying to use different protocol and security level: [{}, {}]. Refusing.", getRemoteAddress(), static_cast<int>(protocol),
                         static_cast<int>(securityLevel));
            disconnect();
            return;
        }
        cfg.protocol = protocol;
        cfg.securityLevel = securityLevel;
        spdlog::info("Connection from {} accepted.", getRemoteAddress());
        spdlog::info("Now using protocol <{}> and security level <{}>.", static_cast<int>(protocol), static_cast<int>(securityLevel));

        initClientListening();
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
        RelayManager::remove(uuid);
        // Client needs to disconnect actively.
    }

    void ServerConnection::routeUpdate(FriendlyByteBuf& buf) {
        const int count = buf.readVarInt();
        for (int i = 0; i < count; ++i) {
            const UUID uuid = buf.readUUID();
            PlayerData data;
            data.x = buf.readDouble();
            data.y = buf.readDouble();
            data.z = buf.readDouble();
            data.dimensionHash = buf.readInt();
            data.spectator = buf.readBool();
            RelayManager::updateData(uuid, data);
        }

        std::unordered_map<UUID, std::vector<int>> playerChannelsSend;
        const int playerChannelCount = buf.readVarInt();
        for (int i = 0; i < playerChannelCount; ++i) {
            const UUID uuid = buf.readUUID();
            const int channelCount = buf.readVarInt();
            std::vector<int> channels;
            channels.reserve(channelCount);
            for (int j = 0; j < channelCount; ++j) {
                channels.push_back(buf.readVarInt());
            }
            playerChannelsSend[uuid] = std::move(channels);
        }

        std::unordered_map<int, std::vector<UUID>> channelPlayersReceive;
        const int channelPlayerCount = buf.readVarInt();
        for (int i = 0; i < channelPlayerCount; ++i) {
            const int channel = buf.readVarInt();
            const int playerCount = buf.readVarInt();
            std::vector<UUID> players;
            players.reserve(playerCount);
            for (int j = 0; j < playerCount; ++j) {
                players.push_back(buf.readUUID());
            }
            channelPlayersReceive[channel] = std::move(players);
        }

        RelayManager::updateChannelData(std::move(playerChannelsSend), std::move(channelPlayersReceive));
    }
} // namespace subspace
