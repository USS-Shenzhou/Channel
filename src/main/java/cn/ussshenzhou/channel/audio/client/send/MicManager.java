package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class MicManager {
    private static TargetDataLine line = null;

    public static void init() {
        var cfg = ChannelClientConfig.get();
        update(cfg.useDevice);
    }

    public static void update(String deviceName) {
        refresh(findMixerByName(deviceName));
    }

    private static Mixer findMixerByName(String name) {
        var mixer = Stream.of(AudioSystem.getMixerInfo())
                .filter(info -> info.getName().equals(name))
                .map(AudioSystem::getMixer)
                .filter(m -> m.getTargetLineInfo().length > 0)
                .findFirst().orElse(null);
        if (mixer != null) {
            return mixer;
        }
        mixer = Stream.of(AudioSystem.getMixerInfo())
                .filter(info -> "DirectAudioDeviceInfo".equals(info.getClass().getSimpleName()) && AudioSystem.getMixer(info).getTargetLineInfo().length > 0)
                .findFirst()
                .map(AudioSystem::getMixer)
                .orElse(null);
        TSimpleNotification.fire(
                Component.translatable("channel.notify.failed_find", name),
                5,
                TSimpleNotification.Severity.WARN
        );
        if (mixer != null) {
            return mixer;
        }
        TSimpleNotification.fire(
                Component.translatable("channel.notify.no_valid"),
                5,
                TSimpleNotification.Severity.ERROR
        );
        return null;
    }

    private static synchronized void refresh(@Nullable Mixer mixer) {
        if (mixer == null) {
            line = null;
            return;
        }
        var format = new AudioFormat(ChannelClientConfig.get().micSampleRate, ModConstant.MIC_SAMPLE_BITS, ModConstant.MIC_CHANNEL, true, false);
        var lineInfo = new DataLine.Info(TargetDataLine.class, format);
        if (!mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
            TSimpleNotification.fire(
                    Component.translatable("channel.notify.unsupported"),
                    5,
                    TSimpleNotification.Severity.ERROR
            );
            line = null;
            return;
        }
        try {
            if (line != null) {
                if (line.isRunning()) {
                    line.stop();
                }
                if (line.isOpen()) {
                    line.close();
                }
            }
            line = (TargetDataLine) mixer.getLine(lineInfo);
            if (!line.isOpen()) {
                line.open(format);
            }
            line.start();
        } catch (Exception e) {
            line = null;
            LogUtils.getLogger().error("{}", e.getMessage());
            TSimpleNotification.fire(
                    Component.translatable("channel.notify.failed_init", mixer.getMixerInfo().getName()),
                    5,
                    TSimpleNotification.Severity.ERROR);
        }
    }

    @Nullable
    public static synchronized TargetDataLine getLine() {
        return line;
    }

    public static int getSampleRate() {
        return line != null ? (int) line.getFormat().getSampleRate() : (int) ChannelClientConfig.get().micSampleRate;
    }
}
