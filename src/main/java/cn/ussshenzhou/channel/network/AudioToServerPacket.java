package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.server.RelayHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = Channel.MODID, handleOnNetwork = true)
public class AudioToServerPacket {
    private final int sampleRate;
    private final byte[] opus;

    public AudioToServerPacket(int sampleRate, byte[] opus) {
        this.sampleRate = sampleRate;
        this.opus = opus;
    }

    @Decoder
    public AudioToServerPacket(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.opus = buf.readByteArray();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeByteArray(this.opus);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        CompletableFuture.runAsync(() -> RelayHandler.process((ServerPlayer) context.player(), opus, sampleRate));
    }
}
