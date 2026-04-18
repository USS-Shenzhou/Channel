package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.audio.client.receive.AudioManagerManager;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
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

    public AudioPacket2C(int sampleRate, UUID from, byte[] opus) {
        this.sampleRate = sampleRate;
        this.from = from;
        this.opus = opus;
    }

    @Decoder
    public AudioPacket2C(FriendlyByteBuf buf) {
        this.sampleRate = buf.readVarInt();
        this.from = buf.readUUID();
        this.opus = buf.readByteArray();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.sampleRate);
        buf.writeUUID(this.from);
        buf.writeByteArray(this.opus);
    }

    @ClientHandler
    public void clientHandler(@Nullable IPayloadContext context) {
        AudioManagerManager.handlePacket(this);
    }
}
