package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.blockentity.ChanneledBlockEntity;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.channel.util.Protocol;
import cn.ussshenzhou.channel.util.SecurityLevel;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = false)
public class SetBlockChannelPacket {
    public final BlockPos pos;
    public final int channel;

    public SetBlockChannelPacket(BlockPos pos, int channel) {
        this.pos = pos;
        this.channel = channel;
    }

    @Decoder
    public SetBlockChannelPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.channel = buf.readVarInt();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(channel);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        var level = context.player().level();
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ChanneledBlockEntity channeledBlockEntity){
            channeledBlockEntity.setChannel(channel);
        }
    }
}
