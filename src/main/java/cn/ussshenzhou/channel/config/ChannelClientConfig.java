package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.audio.Unit;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;

import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public class ChannelClientConfig implements TConfig {

    public String useDevice = "";
    public float networkSampleRate = 24000;
    public float micSampleRate = 48000;
    public int frameLengthMs = 20;
    public boolean listen = false;
    public Trigger trigger = Trigger.THRESHOLD;
    public Vad voiceDetectThreshold = Vad.LOW;
    public float triggerThresholdDBFS = -48;
    public NC noiseCanceling = NC.MID;
    public float aiNoiseCancelingRatio = 0.5f;
    public boolean highPassFilter = true;
    public boolean echoCanceling = false;
    public boolean autoGainControl = true;
    public float forceGainControl = 0;
    public float targetLevel = -5;
    public float maxGain = 20;
    public String nvidiaDllPath = "";
    public Unit unit = Unit.DB;
    public boolean rayTraceAudio = true;
    public float outputAdjust = 0;
    public boolean showHudIcon = true;
    public boolean showHudText = true;
    public int networkTolerance = 200;
    public boolean showRaytrace = false;
    public boolean onAir = true;
    public boolean muteAll = false;
    public boolean hearMyself = false;

    public static ChannelClientConfig get() {
        return ConfigHelper.getConfigRead(ChannelClientConfig.class);
    }

    public static void write(Consumer<ChannelClientConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelClientConfig.class, writer);
    }
}
