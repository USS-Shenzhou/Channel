package cn.ussshenzhou.channel.util;

import java.util.List;

/**
 * @author USS_Shenzhou
 */
public class ModConstant {

    public static final int MIC_SAMPLE_BITS = 16;
    public static final int MIC_CHANNEL = 1;
    public static final List<Float> USABLE_NETWORK_SAMPLE_RATE = List.of(8000f, 12000f, 16000f, 24000f, 48000f);
    public static final List<Integer> USABLE_FRAME_LENGTH = List.of(10, 20, 40, 60);
    public static final int VAD_SMOOTH_WINDOW_LENGTH_MS = 1500;
    public static final int ABS_MIN_DBFS = 96;

    public static final String SHORT_ID = "cha";
}
