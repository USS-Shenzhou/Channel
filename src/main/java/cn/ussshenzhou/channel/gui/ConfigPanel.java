package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.audio.client.send.LevelGatherer;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.audio.client.send.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.util.ImageFit;
import cn.ussshenzhou.t88.gui.widegt.TCycleButton;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TProgressBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

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
    private HorizontalTitledOption<?> thresholdLevel, vad, targetLevel, maxGain, nvidiaLogo, nvidiaCaution, aiNCRatio;

    public ConfigPanel() {
        var cfg = ChannelClientConfig.get();

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
                entry -> entry.getContent().equals(cfg.useDevice)
        ).getB();
        sampleRate = addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.samplerate"),
                List.of(cfg.sampleRate),
                //FIXME change during running
                f -> _ -> ChannelClientConfig.write(c -> c.sampleRate = f),
                entry -> entry.getContent() == cfg.sampleRate
        ).getB();
        sampleRate.setTooltip(Tooltip.create(Component.translatable("channel.config.mic.samplerate.tooltip")));
        addOption(Component.translatable("channel.config.mic.samplebits"), new TLabel(Component.literal(String.valueOf(ModConstant.MIC_SAMPLE_BITS))));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.length"),
                ModConstant.USABLE_FRAME_LENGTH,
                //FIXME change during running
                length -> _ -> ChannelClientConfig.write(c -> c.frameLengthMs = length),
                entry -> entry.getContent() == cfg.frameLengthMs
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.mic.length.tooltip")));
        addOption(
                Component.translatable("channel.config.level"),
                new TProgressBar(90) {
                    {
                        //this.setProgressBarColorGradient(0xff44ff00, 0xffff5900);
                        this.setProgressBarColorGradient(0x003c91ff, 0xff3c91ff);
                        this.setTextMode(new TextMode((_, _, value) -> value == 0 ? "-∞ dBFS" : String.format("%.1f dBFS", value - 90)));
                        this.setTooltip(Tooltip.create(Component.translatable("channel.config.level.raw.tooltip")));
                    }

                    @Override
                    public void tickT() {
                        this.setValue(Mth.clamp(90 + AudioHelper.s2dbfs(LevelGatherer.getRaw()), 0, 90));
                        super.tickT();
                    }
                }
        );

        addOptionSplitter(Component.translatable("channel.config.pre"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.trigger"),
                List.of(Trigger.values()),
                tri -> button -> {
                    ChannelClientConfig.write(c -> c.trigger = tri);
                    button.setTooltip(Tooltip.create(Component.translatable(tri.translateKey() + ".tooltip")));
                    vad.setVisibleT(tri == Trigger.VAD);
                    thresholdLevel.setVisibleT(tri == Trigger.THRESHOLD);
                    ConfigPanel.this.layout();
                },
                entry -> entry.getContent() == cfg.trigger
        ).getB().setTooltip(Tooltip.create(Component.translatable(cfg.trigger.translateKey() + ".tooltip")));
        thresholdLevel = (HorizontalTitledOption<?>) addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.threshold"),
                -90, 0,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dBFS"),
                Component.translatable("channel.config.pre.threshold.tooltip"),
                (slider, _) -> ChannelClientConfig.write(c -> c.triggerThresholdDBFS = (float) slider.getAbsValue()),
                cfg.triggerThresholdDBFS, false
        ).getB().getParent();
        //noinspection DataFlowIssue
        thresholdLevel.setVisibleT(cfg.trigger == Trigger.THRESHOLD);
        var tuple = addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.vad"),
                List.of(Vad.values()),
                //TODO remove button when not needed
                v -> _ -> ChannelClientConfig.write(c -> c.voiceDetectThreshold = v),
                entry -> entry.getContent() == cfg.voiceDetectThreshold
        );
        tuple.getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.vad.tooltip")));
        vad = (HorizontalTitledOption<?>) tuple.getB().getParent();
        //noinspection DataFlowIssue
        vad.setVisibleT(cfg.trigger == Trigger.VAD);
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.nc"),
                List.of(NC.values()),
                n -> _ -> {
                    ChannelClientConfig.write(c -> c.noiseCanceling = n);
                    NvidiaHelper.refresh();
                    WebRTCHelper.refresh();
                    nvidiaLogo.setVisibleT(cfg.noiseCanceling == NC.AI && NvidiaHelper.getStat() == NvidiaHelper.Stat.OK);
                    aiNCRatio.setVisibleT(cfg.noiseCanceling == NC.AI && NvidiaHelper.getStat() == NvidiaHelper.Stat.OK);
                    if (NvidiaHelper.getStat() != NvidiaHelper.Stat.OK) {
                        nvidiaCaution.setVisibleT(cfg.noiseCanceling == NC.AI);
                    }
                    ConfigPanel.this.layout();
                },
                entry -> entry.getContent() == cfg.noiseCanceling
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.nc.tooltip")));
        aiNCRatio = (HorizontalTitledOption<?>) addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.nc.ai_intense"),
                0, 1,
                (_, v) -> Component.literal(String.format("%d", (int) (v * 100)) + "%"),
                Component.translatable("channel.config.pre.nc.ai_intense.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.aiNoiseCancelingRatio = (float) slider.getAbsValue());
                    NvidiaHelper.refresh();
                },
                cfg.aiNoiseCancelingRatio, false
        ).getB().getParent();
        aiNCRatio.setVisibleT(cfg.noiseCanceling == NC.AI && NvidiaHelper.getStat() == NvidiaHelper.Stat.OK);
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.ec"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.echoCanceling = bool);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == cfg.echoCanceling
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.ec.tooltip")));
        nvidiaLogo = (HorizontalTitledOption<?>) addOption(Component.literal("Powered by"), new TImage(ResourceLocation.fromNamespaceAndPath(Channel.MODID, "textures/gui/nvidia.png")) {
            @Override
            public Vector2i getPreferredSize() {
                return new Vector2i(0, 50);
            }
        }).getB().getParent();
        //noinspection DataFlowIssue
        ((TImage) nvidiaLogo.getController()).setImageFit(ImageFit.FIT);
        nvidiaLogo.setVisibleT(cfg.noiseCanceling == NC.AI && NvidiaHelper.getStat() == NvidiaHelper.Stat.OK);
        nvidiaCaution = (HorizontalTitledOption<?>) addOption(Component.empty(), new NvidiaCautionPanel(NvidiaHelper.getStat())).getB().getParent();
        //noinspection DataFlowIssue
        nvidiaCaution.setVisibleT(cfg.noiseCanceling == NC.AI && NvidiaHelper.getStat() != NvidiaHelper.Stat.OK);

        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.hpf"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.highPassFilter = bool);
                    WebRTCHelper.refresh();
                },
                entry -> entry.getContent() == cfg.highPassFilter
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.hpf.tooltip")));
        addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.mgc"),
                -30, 30,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dB"),
                Component.translatable("channel.config.pre.mgc.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.forceGainControl = (float) slider.getAbsValue());
                    WebRTCHelper.refresh();
                },
                cfg.forceGainControl, false
        ).getB().getParent();
        addOptionCycleButtonInit(
                Component.translatable("channel.config.pre.agc"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.autoGainControl = bool);
                    WebRTCHelper.refresh();
                    targetLevel.setVisibleT(bool);
                    maxGain.setVisibleT(bool);
                    ConfigPanel.this.layout();
                },
                entry -> entry.getContent() == cfg.autoGainControl
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.pre.agc.tooltip")));
        targetLevel = (HorizontalTitledOption<?>) addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.target"),
                -26, -2,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dBFS"),
                Component.translatable("channel.config.pre.target.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.targetLevel = (float) slider.getAbsValue());
                    WebRTCHelper.refresh();
                },
                cfg.targetLevel, false
        ).getB().getParent();
        maxGain = (HorizontalTitledOption<?>) addOptionSliderDoubleInit(
                Component.translatable("channel.config.pre.max_gain"),
                0, 30,
                (_, v) -> Component.literal(String.format("%.1f", v) + " dB"),
                Component.translatable("channel.config.pre.max_gain.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.maxGain = (float) slider.getAbsValue());
                    WebRTCHelper.refresh();
                },
                cfg.maxGain, false
        ).getB().getParent();
        targetLevel.setVisibleT(cfg.autoGainControl);
        maxGain.setVisibleT(cfg.autoGainControl);
        addOption(
                Component.translatable("channel.config.level"),
                new TProgressBar(90) {
                    {
                        //this.setProgressBarColorGradient(0xff44ff00, 0xffff5900);
                        this.setProgressBarColorGradient(0x003c91ff, 0xff3c91ff);
                        this.setTextMode(new TextMode((_, _, value) -> value == 0 ? "-∞ dBFS" : String.format("%.1f dBFS", value - 90)));
                        this.setTooltip(Tooltip.create(Component.translatable("channel.config.level.pro.tooltip")));
                    }

                    @Override
                    public void tickT() {
                        this.setValue(Mth.clamp(90 + AudioHelper.s2dbfs(LevelGatherer.getProcessed()), 0, 90));
                        super.tickT();
                    }
                }
        );
        addOptionCycleButtonInit(
                Component.translatable("channel.config.mic.listen"),
                List.of(false, true),
                bool -> _ -> ChannelClientConfig.write(c -> c.listen = bool),
                entry -> entry.getContent() == cfg.listen
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.mic.listen.tooltip")));
        addOptionSplitter(Component.translatable("channel.config.post"));

        updateUsableSelectionByDevice(cfg.useDevice);
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
        supportedSampleRate.forEach(f -> sampleRate.addElement(f, _ -> ChannelClientConfig.write(c -> c.sampleRate = f)));
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
