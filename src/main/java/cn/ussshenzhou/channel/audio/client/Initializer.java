package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.audio.client.receive.AudioManager;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.MicReader;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.audio.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.MicConfirmScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class Initializer {
    private static boolean initialized = false;

    @SubscribeEvent
    public static void initNative(ClientStartedEvent event){
        WebRTCHelper.init();
        NvidiaHelper.init();
    }

    public static void init() {
        if (!initialized) {
            if (ChannelClientConfig.get().cautiousMic) {
                Minecraft.getInstance().setScreen(new MicConfirmScreen());
            } else {
                realInit();
            }
        }
    }

    public static void realInit() {
        if (!initialized) {
            MicManager.init();
            MicReader.init();
            AudioManager.init();
            initialized = true;
        }
    }
}
