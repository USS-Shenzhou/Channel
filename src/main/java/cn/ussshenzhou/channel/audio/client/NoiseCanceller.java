package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.c.NativeLoader;
import cn.ussshenzhou.channel.c.RNNoise;
import cn.ussshenzhou.channel.util.ArrayHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * @author USS_Shenzhou
 */
public class NoiseCanceller {
    private static final MemorySegment DENOISE_STATE;

    static {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            System.load(Path.of(System.getProperty("user.dir"), "../src/main/resources/native/rnnoise/windows/rnnoise.dll").normalize().toAbsolutePath().toString());
        } else {
            NativeLoader.loadRnnoise();
        }
        DENOISE_STATE = RNNoise.rnnoise_create(MemorySegment.NULL);
    }

    public static short[] process(byte[] original) {
        var inArray = ArrayHelper.castS2F(ArrayHelper.reinterpretB2S(original));
        var inSeg = ArrayHelper.heap2DirectAlloc(inArray);
        var outSeg = MemorySegment.ofBuffer(MemoryUtil.memAlloc((int) inSeg.byteSize()));
        RNNoise.rnnoise_process_frame(DENOISE_STATE, outSeg, inSeg);
        var outArray = ArrayHelper.direct2HeapFreeF(outSeg);
        MemoryUtil.nmemFree(inSeg.address());
        return ArrayHelper.castF2S(outArray);
    }
}
