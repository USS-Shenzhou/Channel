package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.util.TimeCounter;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
public class OpusManager {


    private record Encoder(int sampleRate, OpusEncoder encoder) {
    }

    private record Decoder(int sampleRate, OpusDecoder decoder) {
    }

    private static Encoder encoder;
    private static HashMap<UUID, Decoder> decoders = new HashMap<>();
    public static final TimeCounter SEND_SPEED = new TimeCounter(1000);

    public static byte[] encode(byte[] audio, int sampleRate) throws OpusException {
        if (encoder == null || encoder.sampleRate != sampleRate) {
            encoder = new Encoder(sampleRate, new OpusEncoder(sampleRate, 1, sampleRate <= 16000 ? OpusApplication.OPUS_APPLICATION_VOIP : OpusApplication.OPUS_APPLICATION_AUDIO));
        }
        byte[] result = new byte[audio.length];
        var length = encoder.encoder.encode(audio, 0, audio.length / 2, result, 0, result.length);
        SEND_SPEED.put(length);
        return Arrays.copyOf(result, length);
    }

    public static short[] decode(byte[] opus, int sampleRate, UUID from) throws OpusException {
        var length = OpusPacketInfo.getNumSamples(opus, 0, opus.length, sampleRate);
        var outArray = new short[length];
        if (!decoders.containsKey(from) || decoders.get(from).sampleRate != sampleRate) {
            try {
                decoders.put(from, new Decoder(sampleRate, new OpusDecoder(sampleRate, 1)));
            } catch (Exception e) {
                LogUtils.getLogger().error(e.getMessage());
            }
        }
        decoders.get(from).decoder.decode(opus, 0, opus.length, outArray, 0, length, false);
        return outArray;
    }

    public static int getOpusBitRate() {
        return encoder == null ? 0 : encoder.encoder.getBitrate();
    }

}
