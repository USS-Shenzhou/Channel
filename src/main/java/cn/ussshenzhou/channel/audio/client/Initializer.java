package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.audio.client.receive.AudioManager;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.MicReader;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.audio.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.MicConfirmScreen;
import net.minecraft.client.Minecraft;

/**
 * @author USS_Shenzhou
 */
public class Initializer {
    private static boolean initialized = false;

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
            WebRTCHelper.init();
            NvidiaHelper.init();
            AudioManager.init();
            initialized = true;
        }
    }
}
