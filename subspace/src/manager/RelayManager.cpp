//
// Created by USS_Shenzhou on 2026/5/19.
//

#include "RelayManager.h"

#include "../network/ClientTcpConnection.h"
#include "../util/FriendlyByteBuf.h"

namespace subspace {
    void RelayManager::updateData(const UUID& uuid, const PlayerData& data) {
        std::unique_lock lock(dataLock);
        playerDatas[uuid] = data;
    }

    void RelayManager::registerConnection(const UUID& uuid, const std::shared_ptr<ClientTcpConnection>& connection) {
        std::unique_lock lock(connectionLock);
        connections[uuid] = connection;
    }

    void RelayManager::disconnect(const UUID& uuid, const ClientTcpConnection* toRemove) {
        std::unique_lock lock0(connectionLock);
        std::unique_lock lock1(dataLock);
        auto entry = connections.find(uuid);
        if (entry != connections.end() && entry->second.get() == toRemove) {
            connections.erase(entry);
            playerDatas.erase(uuid);
        }
    }

    void RelayManager::relay(const UUID& from, int sampleRate, const ByteArray& opus) {
        PlayerData fr;
        {
            std::shared_lock lock1(dataLock);
            auto f = playerDatas.find(from);
            if (f == playerDatas.end()) {
                spdlog::warn("Received voice data from unknown player {}", from.toString());
                return;
            }
            fr = f->second;
        }
        auto to = findTargets(from, fr);
        FriendlyByteBuf buf;
        buf.writeVarInt(sampleRate);
        buf.writeUUID(from);
        buf.writeByteArray(opus);
        buf.writeDouble(fr.x);
        buf.writeDouble(fr.y);
        buf.writeDouble(fr.z);
        const auto& bytes = *buf.bytes();
        for (const auto& t : to) {
            t->send(bytes);
        }
    }

    std::vector<std::shared_ptr<ClientTcpConnection>> RelayManager::findTargets(const UUID& from, const PlayerData& fr) {
        std::vector<std::shared_ptr<ClientTcpConnection>> targets;
        std::shared_lock lock0(connectionLock);
        std::shared_lock lock1(dataLock);
        for (const auto& [uuid, to] : playerDatas) {
#ifdef NDEBUG
            if (uuid == from) {
                continue;
            }
#endif
            if (fr.dimensionHash != to.dimensionHash) {
                continue;
            }
            const double dx = fr.x - to.x;
            const double dy = fr.y - to.y;
            const double dz = fr.z - to.z;
            if (dx * dx + dy * dy + dz * dz >= 96 * 96) {
                continue;
            }
            if (fr.spectator && !to.spectator) {
                continue;
            }
            auto toConnection = connections.find(uuid);
            if (toConnection != connections.end()) {
                targets.push_back(toConnection->second);
            }
        }
        return targets;
    }
} // subspace
