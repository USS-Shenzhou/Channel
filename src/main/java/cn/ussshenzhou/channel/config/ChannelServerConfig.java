package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.channel.util.Protocol;
import cn.ussshenzhou.channel.util.SecurityLevel;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;

import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
public class ChannelServerConfig implements TConfig {

    public boolean useSubspace = false;
    public String subspaceAddress = "";
    public int subspaceServerPort = 0;
    public int subspaceClientPort = 0;
    public String subspaceFrequency = "";
    public Protocol subspaceProtocol = Protocol.TCP;
    public SecurityLevel subspaceSecurityLevel = SecurityLevel.MID;

    public static ChannelServerConfig get() {
        return ConfigHelper.getConfigRead(ChannelServerConfig.class);
    }

    public static void write(Consumer<ChannelServerConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelServerConfig.class, writer);
    }

    public String getSubspaceFrequency() {
        if (System.getenv().containsKey("CHANNEL_SUBSPACE_FREQUENCY")) {
            return System.getenv("CHANNEL_SUBSPACE_FREQUENCY");
        }
        return subspaceFrequency;
    }
}
