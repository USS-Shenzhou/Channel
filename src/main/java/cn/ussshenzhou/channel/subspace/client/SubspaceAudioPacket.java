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
    public final double x, y, z;

    public SubspaceAudioPacket(FriendlyByteBuf buf) {
        super(buf);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException();
    }
}
