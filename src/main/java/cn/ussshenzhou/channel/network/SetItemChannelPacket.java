package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.Item.ModItems;
import cn.ussshenzhou.channel.blockentity.ChanneledBlockEntity;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = false)
public class SetItemChannelPacket {
    public final InteractionHand hand;
    public final int channel;

    public SetItemChannelPacket(InteractionHand hand, int channel) {
        this.hand = hand;
        this.channel = channel;
    }

    @Decoder
    public SetItemChannelPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.channel = buf.readVarInt();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(channel);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        var item = context.player().getItemInHand(hand);
        if (item.has(ModItems.CHANNEL.get())) {
            item.set(ModItems.CHANNEL.get(), channel);
        }
    }
}
