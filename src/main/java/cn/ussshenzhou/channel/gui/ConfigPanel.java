package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.MicManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.t88.gui.HudManager;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.widegt.TCycleButton;
import net.minecraft.network.chat.Component;

import javax.sound.sampled.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class ConfigPanel extends TOptionsPanel {
    private final TCycleButton<String> devices;
    private final TCycleButton<Float> sampleRate;
    private final TCycleButton<Integer> sampleBits, channel;

    public ConfigPanel() {
        addOptionSplitter(Component.literal("  Microphone"));
        devices = addOptionCycleButtonInit(
                Component.literal("Device"),
                Stream.of(AudioSystem.getMixerInfo())
                        .filter(info -> "DirectAudioDeviceInfo".equals(info.getClass().getSimpleName()) &&
                                AudioSystem.getMixer(info).getTargetLineInfo().length > 0)
                        .map(Mixer.Info::getName)
                        .toList(),
                deviceName -> button -> {
                    ChannelClientConfig.write(c -> c.useDevice = deviceName);
                    updateUsableSelectionByDevice(deviceName);
                    notifyMicManager();
                },
                entry -> entry.getContent().equals(ChannelClientConfig.get().useDevice)
        ).getB();
        sampleRate = addOptionCycleButtonInit(
                Component.literal("Sample Rate(Hz)"),
                List.of(ChannelClientConfig.get().micSampleRate),
                rate -> button -> {
                    ChannelClientConfig.write(c -> c.micSampleRate = rate);
                    notifyMicManager();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().micSampleRate
        ).getB();
        sampleBits = addOptionCycleButtonInit(
                Component.literal("Sample Bits"),
                List.of(ChannelClientConfig.get().micSampleBits),
                bits -> button -> {
                    ChannelClientConfig.write(c -> c.micSampleBits = bits);
                    notifyMicManager();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().micSampleBits
        ).getB();
        channel = addOptionCycleButtonInit(
                Component.literal("Channel Number"),
                List.of(ChannelClientConfig.get().micChannels),
                channels -> button -> {
                    ChannelClientConfig.write(c -> c.micChannels = channels);
                    notifyMicManager();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().micChannels
        ).getB();

        updateUsableSelectionByDevice(ChannelClientConfig.get().useDevice);
    }

    private void updateUsableSelectionByDevice(String deviceName) {
        var supportedSampleRate = new LinkedHashSet<Float>();
        var supportedSampleBits = new LinkedHashSet<Integer>();
        var supportedChannel = new LinkedHashSet<Integer>();
        var deviceInfo = AudioHelper.getDeviceInfo(deviceName);
        var mixer = AudioSystem.getMixer(deviceInfo);
        Stream.of(mixer.getTargetLineInfo())
                .filter(lineInfo -> lineInfo instanceof DataLine.Info)
                .flatMap(lineInfo -> Stream.of(((DataLine.Info) lineInfo).getFormats()))
                .forEach(audioFormat -> {
                    if (audioFormat.getSampleRate() != -1) {
                        supportedSampleRate.add(audioFormat.getSampleRate());
                    }
                    if (audioFormat.getSampleSizeInBits() != -1) {
                        supportedSampleBits.add(audioFormat.getSampleSizeInBits());
                    }
                    if (audioFormat.getChannels() != -1) {
                        supportedChannel.add(audioFormat.getChannels());
                    }
                });
        if (supportedSampleRate.isEmpty()) {
            supportedSampleRate.addAll(List.of(8000f, 16000f, 22050f, 32000f, 44100f, 48000f));
        }
        if (supportedSampleBits.isEmpty()) {
            supportedSampleBits.addAll(List.of(8, 16));
        }
        if (supportedChannel.isEmpty()) {
            supportedChannel.addAll(List.of(1, 2));
        }
        var oldRate = sampleRate.getSelected().getContent();
        sampleRate.getValues().clear();
        supportedSampleRate.forEach(sampleRate::addElement);
        sampleRate.select(oldRate);
        var oldBits = sampleBits.getSelected().getContent();
        sampleBits.getValues().clear();
        supportedSampleBits.forEach(sampleBits::addElement);
        sampleBits.select(oldBits);
        var oldChannels = channel.getSelected().getContent();
        channel.getValues().clear();
        supportedChannel.forEach(channel::addElement);
        channel.select(oldChannels);
    }

    private void notifyMicManager() {
        AudioFormat format = new AudioFormat(sampleRate.getSelected().getContent(),
                sampleBits.getSelected().getContent(),
                channel.getSelected().getContent(),
                true, false);
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
