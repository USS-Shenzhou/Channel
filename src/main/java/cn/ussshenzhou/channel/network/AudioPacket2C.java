package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.audio.client.receive.AudioReceiveHandler;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true, id = "au")
public class AudioPacket2C {
    public final int sampleRate;
    public final UUID from;
    public final byte[] opus;
    public final int[] channels;

    public AudioPacket2C(int sampleRate, UUID from, byte[] opus, @Nullable IntArraySet channels) {
        this.sampleRate = sampleRate;
        this.from = from;
        this.opus = opus;
        if (channels == null) {
            this.channels = new int[0];
        } else {
            this.channels = channels.toIntArray();
        }
    }

    @Decoder
    public AudioPacket2C(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.from = buf.readUUID();
        this.opus = buf.readByteArray();
        int channelCount = buf.readVarInt();
        this.channels = new int[channelCount];
        for (int i = 0; i < channelCount; i++) {
            this.channels[i] = buf.readVarInt();
        }
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeUUID(this.from);
        buf.writeByteArray(this.opus);
        buf.writeVarInt(this.channels.length);
        for (int channel : this.channels) {
            buf.writeVarInt(channel);
        }
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        AudioReceiveHandler.handle(this);
    }
}
