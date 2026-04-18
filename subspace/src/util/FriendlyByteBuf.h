//
// Created by USS_Shenzhou on 2026/4/10.
//

#ifndef SUBSPACE_FRIENDLYBYTEBUF_H
#define SUBSPACE_FRIENDLYBYTEBUF_H

#include "Util.h"

namespace subspace {
    class FriendlyByteBuf {
    public:
        explicit FriendlyByteBuf() :
            bytesWrite(std::make_shared<ByteArray>()), bytesRead(nullptr) {
        }

        explicit FriendlyByteBuf(ByteArray& bytes) :
            bytesWrite(nullptr), bytesRead(&bytes) {
        }

        int readableBytes() const {
            return bytesRead->size() - pos;
        }

        int readInt();
        int readVarInt();
        float readFloat();
        double readDouble();
        bool readBool();
        std::string readUtf();
        ByteArray readByteArray();
        UUID readUUID();

        template <class Enum> Enum readEnum() {
            return static_cast<Enum>(readVarInt());
        }

        template <class K, class V> std::unordered_map<K, V> readMap(const std::function<K(FriendlyByteBuf& buf)>& keyDecoder,
                                                                     const std::function<V(FriendlyByteBuf& buf)>& valueDecoder) {
            int size = readVarInt();
            std::unordered_map<K, V> map;
            map.reserve(size);
            for (int i = 0; i < size; ++i) {
                K k = keyDecoder(*this);
                V v = valueDecoder(*this);
                map.emplace(std::move(k), std::move(v));
            }
            return map;
        }

        template <class V> std::vector<V> readCollection(std::function<V(FriendlyByteBuf& buf)>& decoder) {
            int size = readVarInt();
            std::vector<V> vec;
            vec.reserve(size);
            for (int i = 0; i < size; ++i) {
                vec.push_back(decoder(*this));
            }
            return vec;
        }

        void ensureWritable(int size) const {
            bytesWrite->resize(size);
        }

        std::shared_ptr<ByteArray> bytes() {
            bytesWrite->resize(pos);
            return bytesWrite;
        }

        void writeInt(int value);
        void writeVarInt(int value);
        void writeFloat(float value);
        void writeDouble(double value);
        void writeBool(bool value);
        void writeUtf(const std::string& value);
        void writeByteArray(const ByteArray& value);
        void writeUUID(UUID value);

        template <class Enum> void writeEnum(Enum value) {
            writeVarInt(static_cast<int>(value));
        }

        template <class K, class V> void writeMap(const std::unordered_map<K, V>& map,
                                                  const std::function<void(const K& key, FriendlyByteBuf& buf)>& keyEncoder,
                                                  const std::function<void(const V& value, FriendlyByteBuf& buf)>& valueEncoder) {
            writeVarInt(map.size());
            for (auto& [k,v] : map) {
                keyEncoder(k, *this);
                valueEncoder(v, *this);
            }
        }

        template <class V> void writeCollection(const std::vector<V>& collection, const std::function<void(const V& value, FriendlyByteBuf& buf)>& encoder) {
            writeVarInt(collection.size());
            for (auto& e : collection) {
                encoder(e, *this);
            }
        }

    private:
        std::shared_ptr<ByteArray> bytesWrite;
        ByteArray* bytesRead;
        int pos = 0;

        void ensureReadable(int need) const {
            if (readableBytes() < need) {
                throw std::runtime_error("Read out of bounds");
            }
        }

        void writeRaw(const byte* data, int len) {
            if (static_cast<int>(bytesWrite->size()) < pos + len) {
                bytesWrite->resize(pos + len);
            }
            std::memcpy(bytesWrite->data() + pos, data, len);
            pos += len;
        }

        static int local2BigEnd(int v) {
            if constexpr (std::endian::native == std::endian::little) {
                return std::byteswap(v);
            }
            return v;
        }

        static int64_t local2BigEnd(int64_t v) {
            if constexpr (std::endian::native == std::endian::little) {
                return std::byteswap(v);
            }
            return v;
        }

        static int big2LocalEnd(const byte* b) {
            int v;
            std::memcpy(&v, b, 4);
            return local2BigEnd(v);
        }

        static int64_t big2LocalEnd8(const byte* b) {
            int64_t v;
            std::memcpy(&v, b, 8);
            return local2BigEnd(v);
        }
    };
} // subspace

#endif //SUBSPACE_FRIENDLYBYTEBUF_H
