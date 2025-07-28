package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.c.NativeLoader;
import cn.ussshenzhou.channel.util.ArrayHelper;
import net.elytrium.rnnoise.DenoiseState;

/**
 * @author USS_Shenzhou
 */
public class NoiseCanceller {
    private static final DenoiseState DENOISE_STATE;

    static {
        NativeLoader.loadRnnoise();
        DENOISE_STATE = new DenoiseState();
    }

    public static short[] process(byte[] original) {
        var inArray = ArrayHelper.castS2F(ArrayHelper.reinterpretB2S(original));
        var outArray = new float[inArray.length];
        DENOISE_STATE.processFrame(outArray, inArray);
        return ArrayHelper.castF2S(outArray);
    }
}
