package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.util.ModConstant;
import dev.onvoid.webrtc.media.audio.AudioProcessing;
import dev.onvoid.webrtc.media.audio.AudioProcessingConfig;
import dev.onvoid.webrtc.media.audio.AudioProcessingStreamConfig;
import dev.onvoid.webrtc.media.audio.VoiceActivityDetector;
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

    public static void init() {
        loadNative();
        refresh();
        detector = new VoiceActivityDetector();
        slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / ChannelClientConfig.get().frameLengthMs);
    }

    public static void loadNative() {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            System.load(Path.of(System.getProperty("user.dir"), "../run/webrtc-java.dll").normalize().toAbsolutePath().toString());
            return;
        }
        //TODO
    }

    public static void refresh() {
        synchronized (WebRTCHelper.class) {
            if (processor != null) {
                processor.dispose();
            }
            var cfg = ChannelClientConfig.get();
            processor = new AudioProcessing();
            var config = new AudioProcessingConfig();
            if (cfg.noiseCanceling != NC.OFF) {
                config.noiseSuppression.enabled = true;
                config.noiseSuppression.level = AudioProcessingConfig.NoiseSuppression.Level.values()[cfg.noiseCanceling.ordinal()];
            } else {
                config.noiseSuppression.enabled = false;
            }
            config.highPassFilter.enabled = cfg.highPassFilter;
            if (cfg.echoCanceling) {
                config.echoCanceller.enabled = true;
                config.echoCanceller.enforceHighPassFiltering = true;
                processor.setStreamDelayMs(80);
            } else {
                config.echoCanceller.enabled = false;
                processor.setStreamDelayMs(0);
            }
            if (cfg.autoGainControl) {
                config.gainControl.enabled = true;
                config.gainControl.adaptiveDigital.enabled = true;
                config.gainControl.adaptiveDigital.headroomDb = -cfg.targetLevel;
                config.gainControl.adaptiveDigital.maxGainDb = cfg.maxGain;
                config.gainControl.adaptiveDigital.initialGainDb = 0;
                config.gainControl.adaptiveDigital.maxOutputNoiseLevelDbfs = -40;
                config.gainControl.adaptiveDigital.maxGainChangeDbPerSecond = 12;
            } else {
                config.gainControl.enabled = false;
            }
            processor.applyConfig(config);
        }
    }

    @Nullable
    public static byte[] process(byte[] raw) {
        synchronized (WebRTCHelper.class) {
            var cfg = ChannelClientConfig.get();
            int sampleRate = (int) cfg.sampleRate;
            boolean vad = false;
            var seg = cfg.frameLengthMs / 10;
            var length = raw.length / seg;
            byte[] result = new byte[raw.length];
            for (int i = 0; i < seg; i++) {
                var subRaw = Arrays.copyOfRange(raw, length * i, length * i + length);
                var subResult = new byte[length];
                processor.processStream(
                        subRaw,
                        new AudioProcessingStreamConfig(sampleRate, ModConstant.MIC_CHANNEL),
                        new AudioProcessingStreamConfig(sampleRate, ModConstant.MIC_CHANNEL),
                        subResult
                );
                System.arraycopy(subResult, 0, result, length * i, length);
                vad |= vad(subResult, sampleRate);
            }
            return vad ? result : null;
        }
    }

    private static boolean vad(byte[] audio, int sampleRate) {
        if (ChannelClientConfig.get().trigger != Trigger.VAD) {
            return true;
        }
        var vadLevel = ChannelClientConfig.get().voiceDetectThreshold;
        if (vadLevel == Vad.LOW) {
            return vadInternal(audio, sampleRate) >= 0.01;
        }
        slidingWindow.update(vadInternal(audio, sampleRate) >= ChannelClientConfig.get().voiceDetectThreshold.ordinal() * 0.1f);
        return slidingWindow.getSmoothedValue();
    }

    private static float vadInternal(byte[] audio, int sampleRate) {
        detector.process(audio, audio.length / 2, sampleRate);
        return detector.getLastVoiceProbability();
    }

    public static void updateSlideWindow() {
        slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / ChannelClientConfig.get().frameLengthMs, slidingWindow);
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
