package cn.ussshenzhou.channel.subspace;

import io.netty.buffer.ByteBuf;
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
public class AesCtrDecoder extends AesGcmDecoder {
    public AesCtrDecoder(String token) {
        super(token);
    }

    public AesCtrDecoder(byte[] token) {
        super(token);
    }

    @Override
    protected void initCipher(ByteBuf in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        int nonce = VarInt.read(in);
        var n = Util.getNonce(nonce);

        if (cipher == null) {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
        }
        cipher.init(Cipher.DECRYPT_MODE, token, new IvParameterSpec(n));
    }
}
