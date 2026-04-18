package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author USS_Shenzhou
 */
public class OnceAesGcmEncoder extends AesGcmEncoder {
    public OnceAesGcmEncoder(String token) {
        super(token);
    }

    public OnceAesGcmEncoder(byte[] token) {
        super(token);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        initCipher();
        int outputSize = cipher.getOutputSize(msg.readableBytes());
        out.ensureWritable(outputSize);
        out.writerIndex(out.writerIndex() + cipher.doFinal(msg.nioBuffer(), out.nioBuffer(out.writerIndex(), outputSize)));
        ctx.pipeline().remove(this);
    }
}
