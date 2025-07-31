package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.audio.client.nativ.NvidiaHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * @author USS_Shenzhou
 */

@EventBusSubscriber(Dist.CLIENT)
public class Initializer {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        MicManager.init();
        MicReader.init();
        WebRTCHelper.init();
        NvidiaHelper.init();
    }
}
