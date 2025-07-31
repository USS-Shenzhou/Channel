package cn.ussshenzhou.channel.util;

import cn.ussshenzhou.t88.gui.HudManager;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class AudioHelper {

    @Nullable
    public static Mixer.Info getDeviceInfo(String deviceName) {
        var deviceInfo = Stream.of(AudioSystem.getMixerInfo())
                .filter(info -> info.getName().equals(deviceName))
                .findFirst().orElse(null);
        if (deviceInfo == null) {
            TSimpleNotification.fire(
                    Component.literal("Failed To Find Device: " + deviceName),
                    5,
                    TSimpleNotification.Severity.ERROR
            );
        }
        return deviceInfo;
    }

    public static float s2dbfs(short value) {
        return (float) (20 * Math.log10((value + 1) / 32768f));
    }
}
