package cn.ussshenzhou.channel.util;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.*;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.util.Util;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
public class OpusHelper {

    //FIXME opus is a stateful en/decoder!
    private static final Map<Integer, OpusEncoder> ENCODERS = new HashMap<>();
    private static final Map<Integer, OpusDecoder> DECODERS = new HashMap<>();

    static {
        ModConstant.USABLE_NETWORK_SAMPLE_RATE.forEach(sampleRate -> {
            try {
                int rate = (int) (float) sampleRate;
                ENCODERS.put(rate, new OpusEncoder(rate, 1, rate <= 16000 ? OpusApplication.OPUS_APPLICATION_VOIP : OpusApplication.OPUS_APPLICATION_AUDIO));
                DECODERS.put(rate, new OpusDecoder(rate, 1));
            } catch (OpusException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static byte[] encode(byte[] audio, int sampleRate) throws OpusException {
        byte[] result = new byte[audio.length];
        var length = ENCODERS.get(sampleRate).encode(audio, 0, audio.length / 2, result, 0, result.length);
        return Arrays.copyOf(result, length);
    }

    public static short[] decode(byte[] opus, int sampleRate) throws OpusException {
        var length = OpusPacketInfo.getNumSamples(opus, 0, opus.length, sampleRate);
        var outArray = new short[length];
        DECODERS.get(sampleRate).decode(opus, 0, opus.length, outArray, 0, length, false);
        return outArray;
    }
}
