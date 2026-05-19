package cn.ussshenzhou.channel.subspace.client;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author USS_Shenzhou
 */
public class HandshakePacket extends SubspacePacket {

    public HandshakePacket() {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(Minecraft.getInstance().player.getUUID());
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException();
    }
}
