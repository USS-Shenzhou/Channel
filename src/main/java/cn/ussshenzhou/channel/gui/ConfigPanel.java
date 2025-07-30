package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.audio.client.MicManager;
import cn.ussshenzhou.channel.audio.client.MicReader;
import cn.ussshenzhou.channel.audio.client.WebRTCHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.widegt.TCycleButton;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import net.minecraft.client.gui.components.Tooltip;
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
        ChannelClientConfig.write(c -> c.sampleRate = f);
        notifyMicManager();
    };
    private TOptionsPanel vad, targetLevel, maxGain;

    public ConfigPanel() {
        addOptionSplitter(Component.translatable("channel.config.mic"));
        devices = addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.device"),
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
                Component.translatable("channel.config.mic.samplerate"),
                List.of(ChannelClientConfig.get().sampleRate),
                rate -> _ -> sampleRateRespond.accept(rate),
                entry -> entry.getContent() == ChannelClientConfig.get().sampleRate
        ).getB();
        sampleRate.setTooltip(Tooltip.create(Component.translatable("channel.config.mic.samplerate.tooltip")));
        addOption(Component.translatable("channel.config.mic.samplebits"), new TLabel(Component.literal(String.valueOf(ModConstant.MIC_SAMPLE_BITS))));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.length"),
                ModConstant.USABLE_FRAME_LENGTH,
                length -> _ -> {
                    ChannelClientConfig.write(c -> c.frameLengthMs = length);
                    MicReader.frameLengthChange();
                    WebRTCHelper.updateSlideWindow();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().frameLengthMs
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.mic.length.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.listen"),
                List.of(false, true),
                bool -> _ -> ChannelClientConfig.write(c -> c.listen = bool),
                entry -> entry.getContent() == ChannelClientConfig.get().listen
        );

        addOptionSplitter(Component.translatable("channel.config.pre"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.trigger"),
                List.of(Trigger.values()),
                tri -> _ -> ChannelClientConfig.write(c -> c.trigger = tri),
                entry -> entry.getContent() == ChannelClientConfig.get().trigger
        );
        var tuple = addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.vad"),
                List.of(Vad.values()),
                //TODO remove button when not needed
                v -> _ -> ChannelClientConfig.write(c -> c.voiceDetectThreshold = v),
                entry -> entry.getContent() == ChannelClientConfig.get().voiceDetectThreshold
        );
        tuple.getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.vad.tooltip")));
        vad = tuple.getA();
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.nc"),
                List.of(NC.values()),
                n -> _ -> {
                    ChannelClientConfig.write(c -> c.noiseCanceling = n);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().noiseCanceling
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.nc.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.hpf"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.highPassFilter = bool);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().highPassFilter
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.hpf.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.ec"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.echoCanceling = bool);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().echoCanceling
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.ec.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.agc"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.autoGainControl = bool);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == ChannelClientConfig.get().autoGainControl
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.agc.tooltip")));
        targetLevel = addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.target"),
                -26, -2,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dBFS"),
                Component.translatable("channel.config.pre.target.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.targetLevel = (float) slider.getAbsValue());
                    WebRTCHelper.refresh();
                },
                ChannelClientConfig.get().targetLevel, false
        ).getA();
        maxGain = addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.max_gain"),
                0, 25,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dB"),
                Component.translatable("channel.config.pre.max_gain.tooltip"),
                (slider, v) -> {
                    ChannelClientConfig.write(c -> c.maxGain = (float) slider.getAbsValue());
                    WebRTCHelper.refresh();
                },
                ChannelClientConfig.get().maxGain, false
        ).getA();

        addOptionSplitter(Component.translatable("channel.config.post"));

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
                    if (audioFormat.getSampleRate() != -1 && ModConstant.USABLE_SAMPLE_RATE.contains(audioFormat.getSampleRate())) {
                        supportedSampleRate.add(audioFormat.getSampleRate());
                    }
                });
        if (supportedSampleRate.isEmpty()) {
            supportedSampleRate.addAll(ModConstant.USABLE_SAMPLE_RATE);
        }
        var oldRate = sampleRate.getSelected().getContent();
        sampleRate.getValues().clear();
        supportedSampleRate.forEach(f -> sampleRate.addElement(f, _ -> sampleRateRespond.accept(f)));
        sampleRate.select(oldRate);
    }

    private void notifyMicManager() {
        AudioFormat format = new AudioFormat(sampleRate.getSelected().getContent(), ModConstant.MIC_SAMPLE_BITS, ModConstant.MIC_CHANNEL, true, false);
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
        MicManager.refresh(deviceInfo, format);
    }

}
