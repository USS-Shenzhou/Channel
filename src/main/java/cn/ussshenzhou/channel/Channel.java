package cn.ussshenzhou.channel;

import cn.ussshenzhou.channel.Item.ModItems;
import cn.ussshenzhou.channel.block.ModBlocks;
import cn.ussshenzhou.channel.blockentity.ModBlockEntityTypes;
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
    public static final float DISTANCE_COMPENSATE = 2;
    public static final float DISTANCE_COMPENSATE_SQR = 2 * 2;

    public Channel(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ConfigHelper.loadConfig(new ChannelClientConfig());
            ConfigHelper.loadConfig(new ChannelPlayerConfig());
        }
        ConfigHelper.loadConfig(new ChannelServerConfig());

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModItems.DATA_COMPONENTS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITIES.register(modEventBus);
    }
    //TODO 参考发光/泛光，声音越大，回声越强
}
