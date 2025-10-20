package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.audio.client.receive.AudioManagerManager;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.MicReader;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.audio.nativ.NvidiaHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;

/**
 * @author USS_Shenzhou
 */

@EventBusSubscriber(Dist.CLIENT)
public class Initializer {

    @SubscribeEvent
    public static void init(ClientStartedEvent event) {
        MicManager.init();
        MicReader.init();
        WebRTCHelper.init();
        NvidiaHelper.init();
        AudioManagerManager.init();
    }
}
