package cn.ussshenzhou.channel.subspace.server.send;

import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author USS_Shenzhou
 */
public class RouteUpdatePacket extends SubspacePacket {

    public RouteUpdatePacket() {
        //TODO
    }

    @Override
    public void encode(FriendlyByteBuf buf) {

    }

    @Override
    public int getId() {
        return 3;
    }
}
