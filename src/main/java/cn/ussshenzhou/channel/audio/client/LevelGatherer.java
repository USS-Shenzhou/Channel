package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.util.ArrayHelper;
import com.google.common.collect.EvictingQueue;

import javax.annotation.Nullable;

/**
 * @author USS_Shenzhou
 */
public class LevelGatherer {
    private static final EvictingQueue<Short> MIC_RAW = EvictingQueue.create(10);
    private static final EvictingQueue<Short> MIC_PROCESSED = EvictingQueue.create(10);

    public static short updateRaw(@Nullable byte[] raw) {
        if (raw == null) {
            synchronized (MIC_RAW) {
                MIC_RAW.add((short) 0);
            }
            return 0;
        }
        var value = ArrayHelper.absAverage(ArrayHelper.reinterpretB2S(raw));
        synchronized (MIC_RAW) {
            MIC_RAW.add(value);
        }
        return value;
    }

    public static short updateProcessed(@Nullable byte[] processed) {
        if (processed == null) {
            synchronized (MIC_PROCESSED) {
                MIC_PROCESSED.add((short) 0);
            }
            return 0;
        }
        var value = ArrayHelper.absAverage(ArrayHelper.reinterpretB2S(processed));
        synchronized (MIC_PROCESSED) {
            MIC_PROCESSED.add(value);
        }
        return value;
    }

    public static short getRaw() {
        if (MIC_RAW.isEmpty()) {
            return 0;
        }
        synchronized (MIC_RAW) {
            return ArrayHelper.absAverage(MIC_RAW);
        }
    }

    public static short getProcessed() {
        if (MIC_PROCESSED.isEmpty()) {
            return 0;
        }
        synchronized (MIC_PROCESSED) {
            return ArrayHelper.absAverage(MIC_PROCESSED);
        }
    }
}
