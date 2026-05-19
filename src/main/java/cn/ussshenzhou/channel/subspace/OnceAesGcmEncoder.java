package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author USS_Shenzhou
 */
public class OnceAesGcmEncoder extends AesGcmEncoder {
    private ChannelHandler channelHandler;

    public OnceAesGcmEncoder(String token) {
        super(token);
    }

    public OnceAesGcmEncoder(byte[] token) {
        super(token);
    }

    public OnceAesGcmEncoder(String token, ChannelHandler then) {
        super(token);
        this.channelHandler = then;
    }

    public OnceAesGcmEncoder(byte[] token, ChannelHandler then) {
        super(token);
        this.channelHandler = then;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        initCipher();

        cipher.updateAAD(msg.nioBuffer());
        byte[] tag = cipher.doFinal(new byte[0]);
        out.writeBytes(msg);
        out.writeBytes(tag);

        ctx.pipeline().remove(this);
        ctx.pipeline().addLast(channelHandler);
    }
}
