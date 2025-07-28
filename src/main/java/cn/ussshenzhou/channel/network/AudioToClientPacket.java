package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.server.RelayHandler;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = Channel.MODID, handleOnNetwork = true)
public class AudioToClientPacket {
    private final int sampleRate;
    private final int from;
    private final byte[] opus;

    public AudioToClientPacket(int sampleRate, int from, byte[] opus) {
        this.sampleRate = sampleRate;
        this.from = from;
        this.opus = opus;
    }

    @Decoder
    public AudioToClientPacket(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.from = buf.readVarInt();
        this.opus = buf.readByteArray();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeVarInt(this.from);
        buf.writeByteArray(this.opus);
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        CompletableFuture.runAsync(() -> );
    }
}
