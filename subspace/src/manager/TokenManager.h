//
// Created by USS_Shenzhou on 2026/4/11.
//

#ifndef SUBSPACE_TOKENMANAGER_H
#define SUBSPACE_TOKENMANAGER_H

#include <shared_mutex>

#include "../util/Util.h"

namespace subspace {
    class TokenManager {
    public:
        static void init();

        static const ByteArray& getServerToken() {
            return serverToken;
        }

        static void put(const UUID& uuid, const ByteArray& token);
        static void remove(const UUID& uuid);
        static ByteArray get(const UUID& uuid);
        static bool contains(const UUID& uuid);

    private:
        inline static ByteArray serverToken;
        inline static std::shared_mutex clientTokensLock;
        inline static std::unordered_map<UUID, ByteArray> clientTokens;
    };
} // subspace

#endif //SUBSPACE_TOKENMANAGER_H
