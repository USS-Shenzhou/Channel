//
// Created by USS_Shenzhou on 2026/4/11.
//

#include "ShortIdManager.h"

namespace subspace {
    void ShortIdManager::put(const UUID& uuid, int token) {
        std::unique_lock lock(shortIdsLock);
        shortIds.emplace(uuid, token);
    }

    void ShortIdManager::remove(const UUID& uuid) {
        std::unique_lock lock(shortIdsLock);
        shortIds.erase(uuid);
    }

    /**
     * @warning Contains before Get.
     */
    int ShortIdManager::get(const UUID& uuid) {
        std::shared_lock lock(shortIdsLock);
        return shortIds[uuid];
    }

    bool ShortIdManager::contains(const UUID& uuid) {
        std::shared_lock lock(shortIdsLock);
        return shortIds.contains(uuid);
    }
} // subspace
