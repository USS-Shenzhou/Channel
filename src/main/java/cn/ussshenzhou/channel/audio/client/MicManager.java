package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class MicManager {
    private static TargetDataLine line = null;

    @SubscribeEvent
    public static void initMic(FMLClientSetupEvent event) {
        var cfg = ChannelClientConfig.get();
        var deviceName = cfg.useDevice;
        var deviceInfo = AudioHelper.getDeviceInfo(deviceName);
        if (deviceInfo == null) {
            deviceInfo = Stream.of(AudioSystem.getMixerInfo())
                    .filter(info -> "DirectAudioDeviceInfo".equals(info.getClass().getSimpleName()) &&
                            AudioSystem.getMixer(info).getTargetLineInfo().length > 0)
                    .findFirst().orElse(null);
        }
        if (deviceInfo == null) {
            TSimpleNotification.fire(
                    Component.literal("No Input Audio Device Found."),
                    5,
                    TSimpleNotification.Severity.ERROR
            );
            return;
        }
        var name = deviceInfo.getName();
        ChannelClientConfig.write(channelClientConfig -> channelClientConfig.useDevice = name);
        init(deviceInfo, new AudioFormat(cfg.micSampleRate, cfg.micSampleBits, cfg.micChannels, true, false));
    }

    public static void init(Mixer.Info deviceInfo, AudioFormat format) {
        try {
            var lineInfo = new DataLine.Info(TargetDataLine.class, format);
            if (line != null) {
                if (line.isRunning()) {
                    line.stop();
                }
                if (line.isOpen()) {
                    line.close();
                }
            }
            line = (TargetDataLine) AudioSystem.getMixer(deviceInfo).getLine(lineInfo);
            if (!line.isOpen()) {
                line.open(format);
            }
            line.start();
        } catch (LineUnavailableException e) {
            LogUtils.getLogger().error("{}", e.getMessage());
            TSimpleNotification.fire(Component.literal("Failed To Init Device " + deviceInfo.getName()), 5, TSimpleNotification.Severity.ERROR);
        }
    }

    @Nullable
    public static TargetDataLine getLine() {
        return line;
    }
}
