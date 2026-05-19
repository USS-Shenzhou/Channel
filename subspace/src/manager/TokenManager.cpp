//
// Created by USS_Shenzhou on 2026/4/11.
//

#include <openssl/evp.h>

#include "TokenManager.h"

namespace subspace {
    void TokenManager::init() {
        auto frequency = getConfig().subspaceFrequency;
        const std::string salt = "channel";
        serverToken = ByteArray(32);

        PKCS5_PBKDF2_HMAC(
            frequency.data(),
            frequency.size(),
            reinterpret_cast<const unsigned char*>(salt.data()),
            salt.size(),
            943,
            EVP_sha256(),
            32,
            serverToken.data()
            );
    }

    void TokenManager::put(const UUID& uuid, const ByteArray& token) {
        std::unique_lock lock(clientTokensLock);
        clientTokens[uuid] = token;
    }

    void TokenManager::remove(const UUID& uuid) {
        std::unique_lock lock(clientTokensLock);
        clientTokens.erase(uuid);
    }

    /**
     * @warning Contains before Get.
     */
    ByteArray& TokenManager::get(const UUID& uuid) {
        std::shared_lock lock(clientTokensLock);
        auto r = clientTokens.find(uuid);
        if (r == clientTokens.end()) {
            throw std::out_of_range("contains before get");
        }
        return clientTokens[uuid];
    }

    bool TokenManager::contains(const UUID& uuid) {
        std::shared_lock lock(clientTokensLock);
        return clientTokens.contains(uuid);
    }
} // subspace
