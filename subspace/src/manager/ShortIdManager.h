//
// Created by USS_Shenzhou on 2026/4/11.
//

#ifndef SUBSPACE_SHORTIDMANAGER_H
#define SUBSPACE_SHORTIDMANAGER_H

#include <shared_mutex>

#include "../util/Util.h"

namespace subspace {

class ShortIdManager {
public:
    static void put(const UUID& uuid, int id);
    static void remove(const UUID& uuid);
    static int get(const UUID& uuid);
    static bool contains(const UUID& uuid);

private:
    inline static std::shared_mutex shortIdsLock;
    inline static std::unordered_map<UUID, int> shortIds;
};

} // subspace

#endif //SUBSPACE_SHORTIDMANAGER_H
