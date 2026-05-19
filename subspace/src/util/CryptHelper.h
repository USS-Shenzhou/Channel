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
    private:
        static void int2Nonce(int value, byte* out) {
            out[0] = static_cast<byte>(value >> 24);
            out[1] = static_cast<byte>(value >> 16);
            out[2] = static_cast<byte>(value >> 8);
            out[3] = static_cast<byte>(value);
        }

    public:
        inline static const CryptFunc NULL_ENCODE_DECODE = [](const ByteArray& data, const ByteArray& token) -> ByteArray {
            throw DecryptException("Should not be called");
        };
        inline static const CryptFunc NO_ENCODE_DECODE = [](const ByteArray& data, const ByteArray& token) -> ByteArray {
            return data;
        };

        inline static const CryptFunc AES_GCM_DECODE = [](const ByteArray& encrypted, const ByteArray& token) -> ByteArray {
            if (encrypted.size() < 16) {
                throw DecryptException("Invalid packet length: packet too short.");
            }
            auto [counter, nonceLen] = readVarInt(encrypted.data(), encrypted.size());

            byte nonce[12] = {};
            int2Nonce(counter, nonce);

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
                throw DecryptException("AES-GCM verify failed");
            }
            return decrypted;
        };

        inline static const CryptFunc AES_GCM_ONCE_DECODE = [](const ByteArray& encrypted, const ByteArray& token) -> ByteArray {
            if (encrypted.size() < 16) {
                throw DecryptException("Invalid packet length: packet too short");
            }
            byte nonce[12] = {};
            int aadLen = encrypted.size() - 16;

            thread_local auto cipherContext = EVP_CIPHER_CTX_new();
            EVP_DecryptInit_ex(cipherContext, EVP_aes_256_gcm(), nullptr, nullptr, nullptr);
            EVP_DecryptInit_ex(cipherContext, nullptr, nullptr, token.data(), nonce);

            int outLen = 0;
            EVP_DecryptUpdate(cipherContext, nullptr, &outLen, encrypted.data(), aadLen);
            EVP_CIPHER_CTX_ctrl(cipherContext, EVP_CTRL_GCM_SET_TAG, 16, const_cast<byte*>(encrypted.data() + aadLen));
            if (EVP_DecryptFinal_ex(cipherContext, nullptr, &outLen) <= 0) {
                throw DecryptException("AES-GCM verify failed");
            }
            return {};
        };

        inline static const CryptFunc AES_CTR_DECODE = [](const ByteArray& encrypted, const ByteArray& token) -> ByteArray {
            auto [counter, counterLen] = readVarInt(encrypted.data(), encrypted.size());

            byte nonce[16] = {};
            int2Nonce(counter, nonce);

            thread_local auto cipherContext = EVP_CIPHER_CTX_new();
            EVP_DecryptInit_ex(cipherContext, EVP_aes_256_ctr(), nullptr, token.data(), nonce);

            ByteArray decrypted(encrypted.size() - counterLen);
            int outSize = 0;
            EVP_DecryptUpdate(cipherContext,
                              decrypted.data(), &outSize,
                              encrypted.data() + counterLen, decrypted.size());
            int finalSize = 0;
            EVP_DecryptFinal_ex(cipherContext, decrypted.data() + outSize, &finalSize);
            return decrypted;
        };

        static CryptFunc makeAesGcmEncoder() {
            return [counter = std::make_shared<int>(2)](const ByteArray& plaintext, const ByteArray& token) -> ByteArray {
                byte nonce[12] = {};
                int2Nonce(*counter, nonce);

                byte counterBytes[5];
                int counterLen = writeVarInt(*counter, counterBytes);
                *counter += 2;

                thread_local auto cipherContext = EVP_CIPHER_CTX_new();
                EVP_EncryptInit_ex(cipherContext, EVP_aes_256_gcm(), nullptr, nullptr, nullptr);
                EVP_EncryptInit_ex(cipherContext, nullptr, nullptr, token.data(), nonce);

                ByteArray output(counterLen + plaintext.size() + 16);
                std::memcpy(output.data(), counterBytes, counterLen);

                int outSize = 0;
                EVP_EncryptUpdate(cipherContext,
                                  output.data() + counterLen, &outSize,
                                  plaintext.data(), plaintext.size());
                int finalSize = 0;
                EVP_EncryptFinal_ex(cipherContext, output.data() + counterLen + outSize, &finalSize);
                EVP_CIPHER_CTX_ctrl(cipherContext, EVP_CTRL_GCM_GET_TAG, 16, output.data() + counterLen + plaintext.size());

                return output;
            };
        }

        static CryptFunc makeAesCtrEncoder() {
            return [counter = std::make_shared<int>(2)](const ByteArray& plaintext, const ByteArray& token) -> ByteArray {
                byte nonce[16] = {};
                int2Nonce(*counter, nonce);

                byte counterBytes[5];
                int counterLen = writeVarInt(*counter, counterBytes);
                *counter += 2;

                thread_local auto cipherContext = EVP_CIPHER_CTX_new();
                EVP_EncryptInit_ex(cipherContext, EVP_aes_256_ctr(), nullptr, token.data(), nonce);

                ByteArray output(counterLen + plaintext.size());
                std::memcpy(output.data(), counterBytes, counterLen);

                int outSize = 0;
                EVP_EncryptUpdate(cipherContext,
                                  output.data() + counterLen, &outSize,
                                  plaintext.data(), plaintext.size());
                int finalSize = 0;
                EVP_EncryptFinal_ex(cipherContext, output.data() + counterLen + outSize, &finalSize);

                return output;
            };
        }
    };
} // subspace

#endif //SUBSPACE_CRYPTHELPER_H
