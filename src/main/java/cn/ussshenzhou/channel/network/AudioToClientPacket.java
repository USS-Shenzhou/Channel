package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.client.receive.PlayerTalkAudioStreamManager;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = Channel.MODID, handleOnNetwork = true)
public class AudioToClientPacket {
    public final int sampleRate;
    public final int from;
    public final byte[] opus;

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
        CompletableFuture.runAsync(() -> PlayerTalkAudioStreamManager.handle(this));
    }
}
