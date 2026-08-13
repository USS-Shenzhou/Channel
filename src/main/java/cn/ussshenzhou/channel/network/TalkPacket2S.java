package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.audio.DebugManager;
import cn.ussshenzhou.channel.audio.server.RelayHandler;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true, id = "tsp")
public class TalkPacket2S extends SubspacePacket {
    private final byte[] opus;

    public TalkPacket2S(byte[] opus) {
        this.opus = opus;
    }

    @Decoder
    public TalkPacket2S(FriendlyByteBuf buf) {
        this.opus = buf.readByteArray();
    }

    @Override
    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(this.opus);
        DebugManager.MIC_SEND_COUNTER.update();
        DebugManager.sending(opus);
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException();
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        RelayHandler.process((ServerPlayer) context.player(), opus);
    }
}
