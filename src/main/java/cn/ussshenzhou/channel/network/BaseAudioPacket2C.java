package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.audio.client.receive.AudioManagerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
public abstract class BaseAudioPacket2C {
    public final int sampleRate;
    public final int from;
    public final byte[] opus;

    public BaseAudioPacket2C(int sampleRate, int from, byte[] opus) {
        this.sampleRate = sampleRate;
        this.from = from;
        this.opus = opus;
    }

    public BaseAudioPacket2C(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.from = buf.readVarInt();
        this.opus = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeVarInt(this.from);
        buf.writeByteArray(this.opus);
    }

    public void clientHandler(IPayloadContext context) {
        CompletableFuture.runAsync(() -> AudioManagerManager.handlePacket(this));
    }
}
