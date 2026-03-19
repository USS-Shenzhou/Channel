package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.audio.client.receive.AudioManagerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
public abstract class BaseAudioPacket2C {
    public final int sampleRate;
    public final UUID from;
    public final byte[] opus;

    public BaseAudioPacket2C(int sampleRate, UUID from, byte[] opus) {
        this.sampleRate = sampleRate;
        this.from = from;
        this.opus = opus;
    }

    public BaseAudioPacket2C(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.from = buf.readUUID();
        this.opus = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeUUID(this.from);
        buf.writeByteArray(this.opus);
    }

    public void clientHandler(IPayloadContext context) {
        AudioManagerManager.handlePacket(this);
    }
}
