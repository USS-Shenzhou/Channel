//
// Created by USS_Shenzhou on 2026/4/10.
//

#include "FriendlyByteBuf.h"

namespace subspace {
    int FriendlyByteBuf::readInt() {
        ensureReadable(4);
        int value = big2LocalEnd(bytesRead->data() + pos);
        pos += 4;
        return value;
    }

    int FriendlyByteBuf::readVarInt() {
        auto [value, len] = subspace::readVarInt(bytesRead->data() + pos, readableBytes());
        pos += len;
        return value;
    }

    float FriendlyByteBuf::readFloat() {
        return std::bit_cast<float>(readInt());
    }

    double FriendlyByteBuf::readDouble() {
        ensureReadable(8);
        int64_t value = big2LocalEnd8(bytesRead->data() + pos);
        pos += 8;
        return std::bit_cast<double>(value);
    }

    bool FriendlyByteBuf::readBool() {
        ensureReadable(1);
        return (*bytesRead)[pos++] != 0;
    }

    std::string FriendlyByteBuf::readUtf() {
        int length = readVarInt();
        ensureReadable(length);
        std::string value(reinterpret_cast<char*>(bytesRead->data() + pos), length);
        pos += length;
        return value;
    }

    ByteArray FriendlyByteBuf::readByteArray() {
        int length = readVarInt();
        ensureReadable(length);
        ByteArray value(bytesRead->data() + pos, bytesRead->data() + pos + length);
        pos += length;
        return value;
    }

    UUID FriendlyByteBuf::readUUID() {
        ensureReadable(16);
        UUID value = {big2LocalEnd8(bytesRead->data() + pos), big2LocalEnd8(bytesRead->data() + pos + 8)};
        pos += 16;
        return value;
    }

    void FriendlyByteBuf::writeInt(int value) {
        int v = local2BigEnd(value);
        writeRaw(reinterpret_cast<byte*>(&v), 4);
    }

    void FriendlyByteBuf::writeVarInt(int value) {
        byte buf[5];
        int len = subspace::writeVarInt(value, buf);
        writeRaw(buf, len);
    }

    void FriendlyByteBuf::writeFloat(float value) {
        writeInt(std::bit_cast<int>(value));
    }

    void FriendlyByteBuf::writeDouble(double value) {
        int64_t v = local2BigEnd(std::bit_cast<int64_t>(value));
        writeRaw(reinterpret_cast<byte*>(&v), 8);
    }

    void FriendlyByteBuf::writeBool(bool value) {
        byte v = value ? 1 : 0;
        writeRaw(&v, 1);
    }

    void FriendlyByteBuf::writeUtf(const std::string& value) {
        writeVarInt(value.length());
        writeRaw(reinterpret_cast<const byte*>(value.data()), value.length());
    }

    void FriendlyByteBuf::writeByteArray(const ByteArray& value) {
        writeVarInt(value.size());
        writeRaw(value.data(), value.size());
    }

    void FriendlyByteBuf::writeUUID(UUID value) {
        int64_t most = local2BigEnd(value.most);
        int64_t least = local2BigEnd(value.least);
        writeRaw(reinterpret_cast<byte*>(&most), 8);
        writeRaw(reinterpret_cast<byte*>(&least), 8);
    }
} // subspace
