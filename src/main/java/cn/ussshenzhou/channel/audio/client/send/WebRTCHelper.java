package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.util.ModConstant;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jline.utils.Log;

import javax.annotation.Nullable;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static cn.ussshenzhou.channel.audio.nativ.WebRTC.*;

/**
 * @author USS_Shenzhou
 */
public class WebRTCHelper {
    private volatile static MemorySegment processor = null;
    private volatile static MemorySegment vad = null;
    private static SimpleSlidingBooleanWindow slidingWindow = null;
    private volatile static MemorySegment resampler = null;
    private volatile static int inSampleRate, outSampleRate;

    static {
        loadWebRTC();
    }

    public static void init() {
        refresh();
        if (vad != null) {
            FreeVad(vad);
        }
        vad = CreateVad();
        InitVad(vad);
        SetVadMode(vad, ChannelClientConfig.get().voiceDetectThreshold.ordinal());
        slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / MicReader.getFrameLength());
    }

    private static void loadWebRTC() {
        String os;
        String arch;
        String libName;
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osName.contains("win")) {
            os = "windows";
            libName = "webrtc.dll";
        } else if (osName.contains("linux")) {
            os = "linux";
            libName = "webrtc.so";
        } else if (osName.contains("mac")) {
            os = "macos";
            libName = "webrtc.dylib";
        } else {
            throw new RuntimeException("Unsupported OS: " + osName);
        }
        if (!FMLEnvironment.isProduction()) {
            LogUtils.getLogger().warn("RUNNING IN DEV ENV: If you are loading Channel in dev env, you may need to manually extract lib file (e.g. webrtc.dll) in the jar to your run/ dir.");
            System.load(Path.of("").toAbsolutePath().resolve(libName).toString());
            return;
        }
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            arch = "x86_64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "aarch64";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        var resourcePath = "/natives/" + os + "-" + arch + "/" + libName;
        try (var is = WebRTCHelper.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("This should not happen. Native library not found in jar: " + resourcePath);
            }
            var tempDir = Files.createTempDirectory("channel-natives");
            var tempFile = tempDir.resolve(libName);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load WebRTC. This should not happen.", e);
        }
    }

    public static synchronized void refresh() {
        if (slidingWindow != null) {
            slidingWindow = new SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / MicReader.getFrameLength(), slidingWindow);
        }
        if (processor != null) {
            FreeAudioProcessing(processor);
        }
        var cfg = ChannelClientConfig.get();
        processor = CreateAudioProcessing();

        if (cfg.noiseCanceling != NC.AI) {
            if (cfg.noiseCanceling != NC.OFF) {
                SetNoiseSuppression(processor, true, cfg.noiseCanceling.ordinal());
            } else {
                SetNoiseSuppression(processor, false, 0);
            }
            if (cfg.echoCanceling) {
                SetEchoCanceller(processor, true, true);
                SetStreamDelayMs(processor, 80);
            } else {
                SetEchoCanceller(processor, false, false);
                SetStreamDelayMs(processor, 0);
            }
        }
        SetHighPassFilter(processor, cfg.highPassFilter);
        SetGainController(processor,
                true,
                cfg.forceGainControl,
                cfg.autoGainControl,
                -cfg.targetLevel,
                cfg.maxGain,
                0,
                -40,
                15);
    }

    @Nullable
    public static synchronized byte[] process(byte[] raw, int sampleRateIn, int sampleRateOut) {
        boolean vadPass = false;
        var segAmount = MicReader.getFrameLength() / 10;

        var inStepBytes = raw.length / segAmount;
        var samplesPerChannel = sampleRateIn / 100;

        try (var arena = Arena.ofConfined()) {
            var src = arena.allocate(raw.length);
            var dst = arena.allocate(raw.length);
            MemorySegment.copy(raw, 0, src, ValueLayout.JAVA_BYTE, 0, raw.length);
            for (int i = 0; i < segAmount; i++) {
                long offset = (long) i * inStepBytes;
                var subSrc = src.asSlice(offset, inStepBytes);
                var subDest = dst.asSlice(offset, inStepBytes);
                ProcessStream(processor, subSrc, sampleRateIn, ModConstant.MIC_CHANNEL, subDest);
                vadPass |= vad(subDest, sampleRateIn, samplesPerChannel);
            }
            byte[] processed = new byte[raw.length];
            MemorySegment.copy(dst, ValueLayout.JAVA_BYTE, 0, processed, 0, raw.length);
            return vadPass ? resample(processed, sampleRateIn, sampleRateOut) : null;
        }
    }

    private static byte[] resample(byte[] raw, int sampleRateIn, int sampleRateOut) {
        if (sampleRateIn == sampleRateOut) {
            return raw;
        }
        if (resampler == null || inSampleRate != sampleRateIn || outSampleRate != sampleRateOut) {
            if (resampler != null) {
                FreeResampler(resampler);
            }
            resampler = CreateResampler(sampleRateIn, sampleRateOut, ModConstant.MIC_CHANNEL);
            inSampleRate = sampleRateIn;
            outSampleRate = sampleRateOut;
        }
        var segAmount = MicReader.getFrameLength() / 10;
        var inStepBytes = raw.length / segAmount;
        var outStepBytes = (int) ((float) raw.length / sampleRateIn * sampleRateOut / segAmount);
        int totalOutBytes = (int) ((float) raw.length / sampleRateIn * sampleRateOut);
        try (var arena = Arena.ofConfined()) {
            var src = arena.allocate(raw.length);
            var dst = arena.allocate(totalOutBytes);
            MemorySegment.copy(raw, 0, src, ValueLayout.JAVA_BYTE, 0, raw.length);
            for (int i = 0; i < segAmount; i++) {
                long srcOffset = (long) i * inStepBytes;
                long destOffset = (long) i * outStepBytes;
                var subSrc = src.asSlice(srcOffset, inStepBytes);
                var subDest = dst.asSlice(destOffset, outStepBytes);
                Resample(resampler,
                        subSrc, sampleRateIn / 100,
                        subDest, sampleRateOut / 100);
            }
            byte[] result = new byte[totalOutBytes];
            MemorySegment.copy(dst, ValueLayout.JAVA_BYTE, 0, result, 0, totalOutBytes);
            return result;
        }
    }

    private static boolean vad(MemorySegment audio, int sampleRate, int samplesPerChannel) {
        if (ChannelClientConfig.get().trigger != Trigger.VAD) {
            return true;
        }
        int result = ProcessVad(vad, sampleRate, audio, (long) samplesPerChannel);
        if (result < 0) {
            return true;
        }
        slidingWindow.update((result == 1));
        return slidingWindow.getSmoothedValue();
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
