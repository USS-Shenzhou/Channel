package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true, id = "scp")
public class SpeakerPacket2C extends BaseAudioPacket2C {

    public SpeakerPacket2C(int sampleRate, UUID from, byte[] opus) {
        super(sampleRate, from, opus);
    }

    @Decoder
    public SpeakerPacket2C(FriendlyByteBuf buf) {
        super(buf);
    }

    @Encoder
    @Override
    public void encode(FriendlyByteBuf buf) {
        super.encode(buf);
    }

    @ClientHandler
    @Override
    public void clientHandler(IPayloadContext context) {
        super.clientHandler(context);
    }
}
