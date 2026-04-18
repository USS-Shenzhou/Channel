package cn.ussshenzhou.channel.subspace;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author USS_Shenzhou
 */
public class Util {

    public static SecretKey string2Token(String token) {
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(token.toCharArray(), "channel".getBytes(), 943, 256);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] getNonce(int counter) {
        byte[] nonce = new byte[12];
        nonce[0] = (byte) (counter >>> 24);
        nonce[1] = (byte) (counter >>> 16);
        nonce[2] = (byte) (counter >>> 8);
        nonce[3] = (byte) counter;
        return nonce;
    }
}
