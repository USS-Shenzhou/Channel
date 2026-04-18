package cn.ussshenzhou.channel.subspace.client;

import cn.ussshenzhou.channel.network.AudioPacket2C;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
public class SubspaceAudioPacket extends AudioPacket2C {

    public SubspaceAudioPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        super.encode(buf);
    }

    @Override
    public void clientHandler(@Nullable IPayloadContext context) {
        super.clientHandler(context);
    }
}
