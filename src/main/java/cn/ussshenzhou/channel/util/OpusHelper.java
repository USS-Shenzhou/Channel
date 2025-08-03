package cn.ussshenzhou.channel.util;

import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
public class OpusHelper {

    private static final Map<Integer, OpusEncoder> ENCODERS = new HashMap<>();
    private static final Map<Integer, OpusDecoder> DECODERS = new HashMap<>();

    static {
        try {
            for (float rate : ModConstant.USABLE_SAMPLE_RATE) {
                ENCODERS.put((int) rate, new OpusEncoder((int) rate, 1, OpusApplication.OPUS_APPLICATION_AUDIO));
                DECODERS.put((int) rate, new OpusDecoder((int) rate, 1));
            }
        } catch (OpusException e) {
            throw new RuntimeException(e);
        }
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
