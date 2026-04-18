package cn.ussshenzhou.channel.subspace.server.send;

import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
public class PlayerLoginPacket extends SubspacePacket {

    public final byte[] subspaceToken;
    public final UUID uuid;
    public final int id;

    public PlayerLoginPacket(byte[] subspaceToken, UUID uuid, int id) {
        this.subspaceToken = subspaceToken;
        this.uuid = uuid;
        this.id = id;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(subspaceToken);
        buf.writeUUID(uuid);
        buf.writeVarInt(id);
    }

    @Override
    public int getId() {
        return 1;
    }
}
