package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;

import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
public class ChannelClientConfig implements TConfig {

    public String useDevice = "";
    public float micSampleRate = 16000;
    public int frameLengthMs = 20;
    public boolean noiseCanceling = true;


    public static ChannelClientConfig get() {
        return ConfigHelper.getConfigRead(ChannelClientConfig.class);
    }

    public static void write(Consumer<ChannelClientConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelClientConfig.class, writer);
    }
}
