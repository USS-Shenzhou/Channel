package cn.ussshenzhou.channel.subspace.server.send;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author USS_Shenzhou
 */
public class InitPacket extends SubspacePacket {

    public InitPacket() {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        var cfg = ChannelServerConfig.get();
        buf.writeEnum(cfg.subspaceProtocol);
        buf.writeEnum(cfg.subspaceSecurityLevel);
    }

    @Override
    public int getId() {
        return 0;
    }
}
