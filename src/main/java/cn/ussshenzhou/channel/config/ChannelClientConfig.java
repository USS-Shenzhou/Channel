package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;

import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
public class ChannelClientConfig implements TConfig {

    public String useDevice = "";
    public float micSampleRate = 44100;
    public int micSampleBits = 16;
    public int micChannels = 1;
    public int frameLengthMs = 20;


    public static ChannelClientConfig get() {
        return ConfigHelper.getConfigRead(ChannelClientConfig.class);
    }

    public static void write(Consumer<ChannelClientConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelClientConfig.class, writer);
    }
}
