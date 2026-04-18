package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.VarInt;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author USS_Shenzhou
 */
public class AesCtrEncoder extends AesGcmEncoder {
    public AesCtrEncoder(String token) {
        super(token);
    }

    public AesCtrEncoder(byte[] token) {
        super(token);
    }

    @Override
    protected void initCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] nonce = Util.getNonce(0);
        if (cipher == null) {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        cipher.init(Cipher.ENCRYPT_MODE, token, new IvParameterSpec(nonce));
    }
}
