//
// Created by USS_Shenzhou on 2026/4/10.
//

#ifndef SUBSPACE_UTIL_H
#define SUBSPACE_UTIL_H

#include <vector>
#include <functional>
#include <string>
#include <memory>
#include <bit>
#include <stdexcept>
#include <format>
#include <unordered_set>

#include "../Config.h"

namespace subspace {
    using byte = unsigned char;
    using ByteArray = std::vector<byte>;
    using CryptFunc = std::function<ByteArray(const ByteArray& data, const ByteArray& token)>;

    struct UUID {
        int64_t most;
        int64_t least;

        bool operator==(const UUID&) const = default;

        std::string toString() const {
            const auto m = static_cast<uint64_t>(most);
            const auto l = static_cast<uint64_t>(least);
            return std::format(
                "{:08x}-{:04x}-{:04x}-{:04x}-{:012x}",
                (m >> 32) & 0xFFFFFFFFu,
                (m >> 16) & 0xFFFFu,
                m & 0xFFFFu,
                (l >> 48) & 0xFFFFu,
                l & 0xFFFFFFFFFFFFull
            );
        }
    };

    inline std::pair<int, int> readVarInt(const byte* data, int maxLen) {
        int value = 0;
        int offset = 0;
        int i = 0;
        byte b;
        do {
            if (i >= maxLen) {
                throw std::runtime_error("VarInt exceeds available data");
            }
            b = data[i++];
            value |= (b & 0x7F) << offset;
            offset += 7;
            if (offset > 35) {
                throw std::runtime_error("VarInt too big");
            }
        } while ((b & 0x80) != 0);
        return {value, i};
    }

    inline int writeVarInt(int value, byte* out) {
        auto v = static_cast<unsigned int>(value);
        int i = 0;
        while ((v & ~0x7Fu) != 0) {
            out[i++] = static_cast<byte>((v & 0x7F) | 0x80);
            v >>= 7;
        }
        out[i++] = static_cast<byte>(v);
        return i;
    }
} // subspace

template <> struct std::hash<subspace::UUID> {
    size_t operator()(const subspace::UUID& u) const noexcept {
        return std::hash<int64_t>{}(u.most) ^ (std::hash<int64_t>{}(u.least) << 32);
    }
};

#endif //SUBSPACE_UTIL_H
