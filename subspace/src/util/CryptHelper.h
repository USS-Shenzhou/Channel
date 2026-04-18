//
// Created by USS_Shenzhou on 2026/4/9.
//

#ifndef SUBSPACE_CRYPTHELPER_H
#define SUBSPACE_CRYPTHELPER_H

#include <functional>
#include <stdexcept>
#include <openssl/evp.h>

#include "../util/Util.h"

namespace subspace {
    class DecryptException : public std::runtime_error {
        using std::runtime_error::runtime_error;
    };

    class CryptHelper {
    public:
        inline static const CryptFunc NULL_ENCODE_DECODE = [](const ByteArray& data, const ByteArray& token) -> ByteArray {
            throw DecryptException("Should not be called");
        };
        inline static const CryptFunc NO_ENCODE_DECODE = [](const ByteArray& data, const ByteArray& token) -> ByteArray {
            return data;
        };

        inline static const CryptFunc AES_GCM_DECODE = [](const ByteArray& encrypted, const ByteArray& token) -> ByteArray {
            auto [counter, nonceLen] = readVarInt(encrypted.data(), encrypted.size());

            byte nonce[12] = {};
            nonce[0] = static_cast<byte>(counter >> 24);
            nonce[1] = static_cast<byte>(counter >> 16);
            nonce[2] = static_cast<byte>(counter >> 8);
            nonce[3] = static_cast<byte>(counter);

            thread_local auto cipherContext = EVP_CIPHER_CTX_new();
            EVP_DecryptInit_ex(cipherContext, EVP_aes_256_gcm(), nullptr, nullptr, nullptr);
            EVP_DecryptInit_ex(cipherContext, nullptr, nullptr, token.data(), nonce);

            ByteArray decrypted(encrypted.size() - nonceLen - 16);
            int outSize = 0;
            EVP_DecryptUpdate(cipherContext,
                              decrypted.data(), &outSize,
                              encrypted.data() + nonceLen, decrypted.size()
                );
            EVP_CIPHER_CTX_ctrl(cipherContext, EVP_CTRL_GCM_SET_TAG, 16, const_cast<byte*>(encrypted.data() + encrypted.size() - 16));
            int nul = 0;
            if (EVP_DecryptFinal_ex(cipherContext, nullptr, &nul) <= 0) {
                throw DecryptException("AES-GCM verify failed.");
            }
            return decrypted;
        };
    };
} // subspace

#endif //SUBSPACE_CRYPTHELPER_H
