package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.VarInt;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author USS_Shenzhou
 */
public class AesGcmEncoder extends MessageToByteEncoder<ByteBuf> {
    protected final SecretKey token;
    protected Cipher cipher = null;
    protected int counter = 0;

    public AesGcmEncoder(String token) {
        this.token = Util.string2Token(token);
    }

    public AesGcmEncoder(byte[] token) {
        this.token = new SecretKeySpec(token, "AES");
    }

    public AesGcmEncoder(String token, int initNumber) {
        this(token);
        this.counter = initNumber;
    }

    public AesGcmEncoder(byte[] token, int initNumber) {
        this(token);
        this.counter = initNumber;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        initCipher();
        int outputSize = cipher.getOutputSize(msg.readableBytes());
        VarInt.write(out, counter);
        out.ensureWritable(outputSize);
        out.writerIndex(out.writerIndex() + cipher.doFinal(msg.nioBuffer(), out.nioBuffer(out.writerIndex(), outputSize)));
        counter+=2;
    }

    protected void initCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] nonce = Util.getNonce(counter);

        if (cipher == null) {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        cipher.init(Cipher.ENCRYPT_MODE, token, new GCMParameterSpec(128, nonce));
    }

}
