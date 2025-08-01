package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;

import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public class ChannelClientConfig implements TConfig {

    public String useDevice = "";
    public float sampleRate = 16000;
    public int frameLengthMs = 20;
    public boolean listen = false;
    public Trigger trigger = Trigger.VAD;
    public Vad voiceDetectThreshold = Vad.LOW;
    public float triggerThresholdDBFS = -40;
    public NC noiseCanceling = NC.HIGH;
    public float aiNoiseCancelingRatio = 0.5f;
    public boolean highPassFilter = true;
    public boolean echoCanceling = false;
    public boolean autoGainControl = true;
    public float forceGainControl = 0;
    public float targetLevel = -5;
    public float maxGain = 20;


    public static ChannelClientConfig get() {
        return ConfigHelper.getConfigRead(ChannelClientConfig.class);
    }

    public static void write(Consumer<ChannelClientConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelClientConfig.class, writer);
    }
}
