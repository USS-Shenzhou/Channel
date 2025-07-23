package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.c.NativeLoader;
import cn.ussshenzhou.channel.util.ArrayHelper;
import net.elytrium.rnnoise.DenoiseState;
import net.minecraft.SharedConstants;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

/**
 * @author USS_Shenzhou
 */
public class NoiseCanceller {
    private static final DenoiseState DENOISE_STATE;

    static {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            System.load(Path.of(System.getProperty("user.dir"), "../src/main/resources/native/rnnoise/windows_x64/rnnoise.dll").normalize().toAbsolutePath().toString());
        } else {
            NativeLoader.loadRnnoise();
        }
        DENOISE_STATE = new DenoiseState();
    }

    public static short[] process(byte[] original) {
        var inArray = ArrayHelper.castS2F(ArrayHelper.reinterpretB2S(original));
        var outArray = new float[inArray.length];
        DENOISE_STATE.processFrame(outArray, inArray);
        return ArrayHelper.castF2S(outArray);
    }
}
