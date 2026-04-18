package cn.ussshenzhou.channel.subspace.server.send;

import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
public class PlayerLogoutPacket extends SubspacePacket {
    public final UUID uuid;

    public PlayerLogoutPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    @Override
    public int getId() {
        return 2;
    }
}
