package cn.ussshenzhou.channel;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.t88.config.ConfigHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * @author USS_Shenzhou
 */
@Mod(Channel.MODID)
public class Channel {
    public static final String MODID = "channel";

    public Channel(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ConfigHelper.loadConfig(new ChannelClientConfig());
            ConfigHelper.loadConfig(new ChannelPlayerConfig());
        }
        ConfigHelper.loadConfig(new ChannelServerConfig());
    }
    //TODO 参考发光/泛光，声音越大，回声越强
}
