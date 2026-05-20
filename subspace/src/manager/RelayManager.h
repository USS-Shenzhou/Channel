//
// Created by USS_Shenzhou on 2026/5/19.
//

#ifndef SUBSPACE_RELAYMANAGER_H
#define SUBSPACE_RELAYMANAGER_H

#include <shared_mutex>

#include "../util/Util.h"

namespace subspace {
    class ClientTcpConnection;

    struct PlayerData {
        double x = 0;
        double y = 0;
        double z = 0;
        int dimensionHash = 0;
        bool spectator = false;
    };

    class RelayManager {
    public:
        static void updateData(const UUID& uuid, const PlayerData& data);

        static void registerConnection(const UUID& uuid, const std::shared_ptr<ClientTcpConnection>& connection);
        static void disconnect(const UUID& uuid, const ClientTcpConnection* toRemove);

        static void relay(const UUID& from, int sampleRate, const ByteArray& opus);

    private:
        inline static std::shared_mutex dataLock;
        inline static std::unordered_map<UUID, PlayerData> playerDatas;
        inline static std::shared_mutex connectionLock;
        inline static std::unordered_map<UUID, std::shared_ptr<ClientTcpConnection>> connections;

        static std::vector<std::shared_ptr<ClientTcpConnection>> findTargets(const UUID& from, const PlayerData& fr);
    };
} // subspace

#endif //SUBSPACE_RELAYMANAGER_H
