package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.util.ModConstant;
import dev.onvoid.webrtc.media.audio.*;
import net.minecraft.SharedConstants;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author USS_Shenzhou
 */
public class WebRTCHelper {
    private volatile static AudioProcessing processor = null;
    private volatile static VoiceActivityDetector detector = null;
    private static SimpleSlidingBooleanWindow slidingWindow = null;
    private volatile static AudioResampler resampler = null;
    private volatile static int inSampleRate, outSampleRate;

    public static void init() {
        refresh();
        detector = new VoiceActivityDetector();
        slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / MicReader.getFrameLength());
    }

    public static synchronized void refresh() {
        if (processor != null) {
            processor.dispose();
        }
        var cfg = ChannelClientConfig.get();
        processor = new AudioProcessing();
        var config = new AudioProcessingConfig();
        if (cfg.noiseCanceling != NC.AI) {
            if (cfg.noiseCanceling != NC.OFF) {
                config.noiseSuppression.enabled = true;
                config.noiseSuppression.level = AudioProcessingConfig.NoiseSuppression.Level.values()[cfg.noiseCanceling.ordinal()];
            } else {
                config.noiseSuppression.enabled = false;
            }
            if (cfg.echoCanceling) {
                config.echoCanceller.enabled = true;
                config.echoCanceller.enforceHighPassFiltering = true;
                processor.setStreamDelayMs(80);
            } else {
                config.echoCanceller.enabled = false;
                processor.setStreamDelayMs(0);
            }
        }
        config.highPassFilter.enabled = cfg.highPassFilter;
        config.gainControl.enabled = true;
        config.gainControl.fixedDigital.gainDb = cfg.forceGainControl;
        if (cfg.autoGainControl) {
            config.gainControl.adaptiveDigital.enabled = true;
            config.gainControl.adaptiveDigital.headroomDb = -cfg.targetLevel;
            config.gainControl.adaptiveDigital.maxGainDb = cfg.maxGain;
            config.gainControl.adaptiveDigital.initialGainDb = 0;
            config.gainControl.adaptiveDigital.maxOutputNoiseLevelDbfs = -40;
            config.gainControl.adaptiveDigital.maxGainChangeDbPerSecond = 15;
        } else {
            config.gainControl.adaptiveDigital.enabled = false;
        }
        processor.applyConfig(config);
    }

    @Nullable
    public static synchronized byte[] process(byte[] raw, int sampleRateIn, int sampleRateOut) {
        boolean vadPass = false;
        var segAmount = MicReader.getFrameLength() / 10;
        var inStepLength = raw.length / segAmount;
        byte[] processed = new byte[raw.length];
        for (int i = 0; i < segAmount; i++) {
            var subRaw = Arrays.copyOfRange(raw, inStepLength * i, inStepLength * (i + 1));
            var subResult = new byte[inStepLength];
            processor.processStream(
                    subRaw,
                    new AudioProcessingStreamConfig(sampleRateIn, ModConstant.MIC_CHANNEL),
                    new AudioProcessingStreamConfig(sampleRateIn, ModConstant.MIC_CHANNEL),
                    subResult
            );
            System.arraycopy(subResult, 0, processed, inStepLength * i, inStepLength);
            vadPass |= vad(subResult, sampleRateIn);
        }
        return vadPass ? resample(processed, sampleRateIn, sampleRateOut) : null;
    }

    private static byte[] resample(byte[] raw, int sampleRateIn, int sampleRateOut) {
        if (sampleRateIn == sampleRateOut) {
            return raw;
        }
        if (resampler == null || inSampleRate != sampleRateIn || outSampleRate != sampleRateOut) {
            resampler = new AudioResampler(sampleRateIn, sampleRateOut, ModConstant.MIC_CHANNEL);
            inSampleRate = sampleRateIn;
            outSampleRate = sampleRateOut;
        }
        var seg = MicReader.getFrameLength() / 10;
        var inStepLength = raw.length / seg;
        var outStepLength = (int) ((float) raw.length / sampleRateIn * sampleRateOut / seg);
        byte[] result = new byte[(int) ((float) raw.length / sampleRateIn * sampleRateOut)];
        for (int i = 0; i < seg; i++) {
            var subRaw = Arrays.copyOfRange(raw, inStepLength * i, inStepLength * (i + 1));
            var subResult = new byte[outStepLength];
            resampler.resample(subRaw, sampleRateIn / 100, subResult, sampleRateOut / 100, ModConstant.MIC_CHANNEL);
            System.arraycopy(subResult, 0, result, outStepLength * i, outStepLength);
        }
        return result;
    }

    private static boolean vad(byte[] audio, int sampleRate) {
        if (ChannelClientConfig.get().trigger != Trigger.VAD) {
            return true;
        }
        var vadLevel = ChannelClientConfig.get().voiceDetectThreshold;
        if (vadLevel == Vad.LOW) {
            return vadInternal(audio, sampleRate) >= 0.005;
        }
        slidingWindow.update(vadInternal(audio, sampleRate) >= ChannelClientConfig.get().voiceDetectThreshold.ordinal() * 0.1f);
        return slidingWindow.getSmoothedValue();
    }

    private static float vadInternal(byte[] audio, int sampleRate) {
        detector.process(audio, audio.length / 2, sampleRate);
        return detector.getLastVoiceProbability();
    }

    public static void updateSlideWindow() {
        slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / MicReader.getFrameLength(), slidingWindow);
    }

    public static class SimpleSlidingBooleanWindow {
        private final boolean[] buffer;
        private int head = 0;
        private int size = 0;
        private int trueCount = 0;

        public SimpleSlidingBooleanWindow(int capacity) {
            buffer = new boolean[capacity];
        }

        public SimpleSlidingBooleanWindow(int capacity, SimpleSlidingBooleanWindow old) {
            buffer = new boolean[capacity];
            int copyCount = Math.min(capacity, old.size);
            int start = capacity - copyCount;
            for (int i = 0; i < copyCount; i++) {
                int oldIdx = (old.head - old.size + i + old.buffer.length) % old.buffer.length;
                boolean val = old.buffer[oldIdx];
                buffer[start + i] = val;
                if (val) {
                    trueCount++;
                }
            }
            size = copyCount;
            head = (start + copyCount) % capacity;
        }

        public void update(boolean value) {
            if (size == buffer.length) {
                boolean old = buffer[head];
                if (old) {
                    trueCount--;
                }
            } else {
                size++;
            }
            buffer[head] = value;
            if (value) {
                trueCount++;
            }
            head = (head + 1) % buffer.length;
        }

        public boolean getSmoothedValue() {
            return trueCount > 0;
        }
    }
}
