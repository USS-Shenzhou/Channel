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
        refresh(deviceInfo, new AudioFormat(cfg.micSampleRate, ModConstant.MIC_SAMPLE_BITS, ModConstant.MIC_CHANNEL, true, false));
    }

    public static synchronized void refresh(Mixer.Info deviceInfo, AudioFormat format) {
        var lineInfo = new DataLine.Info(TargetDataLine.class, format);
        var mixer = AudioSystem.getMixer(deviceInfo);
        if (!mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
            TSimpleNotification.fire(
                    Component.literal("Selected Device Parameters Are Not Supported."),
                    5,
                    TSimpleNotification.Severity.ERROR
            );
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
            TSimpleNotification.fire(Component.literal("Failed To Init Device " + deviceInfo.getName()), 5, TSimpleNotification.Severity.ERROR);
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
