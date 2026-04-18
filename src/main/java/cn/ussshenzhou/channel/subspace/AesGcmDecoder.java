package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.network.VarInt;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author USS_Shenzhou
 */
public class AesGcmDecoder extends ByteToMessageDecoder {
    protected final SecretKey token;
    protected Cipher cipher = null;

    public AesGcmDecoder(String token) {
        this.token = Util.string2Token(token);
    }

    public AesGcmDecoder(byte[] token) {
        this.token = new SecretKeySpec(token, "AES");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        initCipher(in);
        var encrypted = new byte[in.readableBytes()];
        in.readBytes(encrypted);
        var decrypted = cipher.doFinal(encrypted);
        out.add(Unpooled.wrappedBuffer(decrypted));
    }

    protected void initCipher(ByteBuf in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        int nonce = VarInt.read(in);
        var n = Util.getNonce(nonce);

        if (cipher == null) {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        cipher.init(Cipher.DECRYPT_MODE, token, new GCMParameterSpec(128, n));
    }
}
