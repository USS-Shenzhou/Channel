package cn.ussshenzhou.channel;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.t88.config.ConfigHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * @author USS_Shenzhou
 */
@Mod(Channel.MODID)
public class Channel {
    public static final String MODID = "channel";

    public Channel(IEventBus modEventBus, ModContainer modContainer) {
        ConfigHelper.loadConfig(new ChannelClientConfig());
        ConfigHelper.loadConfig(new ChannelServerConfig());
    }
}
