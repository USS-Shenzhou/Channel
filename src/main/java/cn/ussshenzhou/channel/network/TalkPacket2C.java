package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true, id = "tcp")
public class TalkPacket2C extends BaseAudioPacket2C {

    public TalkPacket2C(int sampleRate, int from, byte[] opus) {
        super(sampleRate, from, opus);
    }

    @Decoder
    public TalkPacket2C(FriendlyByteBuf buf) {
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
