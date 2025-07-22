package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.MicManager;
import cn.ussshenzhou.channel.audio.client.MicReader;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.widegt.TCycleButton;
import net.minecraft.network.chat.Component;

import javax.sound.sampled.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class ConfigPanel extends TOptionsPanel {
    private final TCycleButton<String> devices;
    private final TCycleButton<Float> sampleRate;
    private final Consumer<Float> sampleRateRespond = f -> {
        ChannelClientConfig.write(c -> c.micSampleRate = f);
        notifyMicManager();
    };

    public ConfigPanel() {
        addOptionSplitter(Component.literal("  Microphone"));
        devices = addOptionCycleButtonInit(
                Component.literal("Device"),
                Stream.of(AudioSystem.getMixerInfo())
                        .filter(info -> "DirectAudioDeviceInfo".equals(info.getClass().getSimpleName()) &&
                                AudioSystem.getMixer(info).getTargetLineInfo().length > 0)
                        .map(Mixer.Info::getName)
                        .toList(),
                deviceName -> _ -> {
                    ChannelClientConfig.write(c -> c.useDevice = deviceName);
                    updateUsableSelectionByDevice(deviceName);
                    notifyMicManager();
                },
                entry -> entry.getContent().equals(ChannelClientConfig.get().useDevice)
        ).getB();
        sampleRate = addOptionCycleButtonInit(
                Component.literal("Sample Rate(Hz)"),
                List.of(ChannelClientConfig.get().micSampleRate),
                rate -> _ -> sampleRateRespond.accept(rate),
                entry -> entry.getContent() == ChannelClientConfig.get().micSampleRate
        ).getB();
        /*TODO Rnnoise only accept 20ms
           addOptionCycleButtonInit(
                Component.literal("Frame Length"),
                List.of(5, 10, 20, 40, 60),
                length -> _ -> {
                    ChannelClientConfig.write(c -> c.frameLengthMs = length);
                    MicReader.frameLengthChange();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().frameLengthMs
        )*/;

        updateUsableSelectionByDevice(ChannelClientConfig.get().useDevice);
    }

    private void updateUsableSelectionByDevice(String deviceName) {
        var supportedSampleRate = new LinkedHashSet<Float>();
        var deviceInfo = AudioHelper.getDeviceInfo(deviceName);
        var mixer = AudioSystem.getMixer(deviceInfo);
        Stream.of(mixer.getTargetLineInfo())
                .filter(lineInfo -> lineInfo instanceof DataLine.Info)
                .flatMap(lineInfo -> Stream.of(((DataLine.Info) lineInfo).getFormats()))
                .forEach(audioFormat -> {
                    if (audioFormat.getSampleRate() != -1 && ModConstant.SAMPLE_RATE.contains(audioFormat.getSampleRate())) {
                        supportedSampleRate.add(audioFormat.getSampleRate());
                    }
                });
        if (supportedSampleRate.isEmpty()) {
            supportedSampleRate.addAll(ModConstant.SAMPLE_RATE);
        }
        var oldRate = sampleRate.getSelected().getContent();
        sampleRate.getValues().clear();
        supportedSampleRate.forEach(f -> sampleRate.addElement(f, _ -> sampleRateRespond.accept(f)));
        sampleRate.select(oldRate);
    }

    private void notifyMicManager() {
        AudioFormat format = new AudioFormat(sampleRate.getSelected().getContent(),
                ModConstant.MIC_SAMPLE_BITS, ModConstant.MIC_CHANNEL, true, false);
        var deviceName = devices.getSelected().getContent();
        var deviceInfo = AudioHelper.getDeviceInfo(deviceName);
        if (deviceInfo == null) {
            return;
        }
        if (!AudioSystem.getMixer(deviceInfo).isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
            TSimpleNotification.fire(
                    Component.literal("Selected Device Parameters Are Not Supported."),
                    5,
                    TSimpleNotification.Severity.ERROR
            );
            return;
        }
        MicManager.init(deviceInfo, format);
    }
}
