package cn.ussshenzhou.channel.subspace.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.FriendlyByteBuf;

public class SubspacePacketHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        var buf = new FriendlyByteBuf(msg);
        var packet = new SubspaceAudioPacket(buf);
        packet.clientHandler(null);
    }
}
