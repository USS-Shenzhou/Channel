package cn.ussshenzhou.channel.subspace;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @author USS_Shenzhou
 */
public abstract class SubspacePacket {

    public void encode(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException();
    }


    public SubspacePacket decode(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException();
    }


    public abstract int getId();
}
