package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.client.send.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.network.AudioToServerPacket;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.channel.util.OpusHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author USS_Shenzhou
 */
public class MicReader {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> keepReading;
    private static WebRTCHelper.SimpleSlidingBooleanWindow slidingWindow = null;
    private static int frameLength = 0;

    public static void init() {
        frameLength = ChannelClientConfig.get().frameLengthMs;
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, frameLength, TimeUnit.MILLISECONDS);
        slidingWindow = new WebRTCHelper.SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / frameLength);
    }

    @Deprecated
    public static void frameLengthChange() {
        synchronized (MicReader.class) {
            keepReading.cancel(false);
            frameLength = ChannelClientConfig.get().frameLengthMs;
            keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, frameLength, TimeUnit.MILLISECONDS);
            slidingWindow = new WebRTCHelper.SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / frameLength, slidingWindow);
        }
    }

    private static void read() {
        synchronized (MicReader.class) {
            if (Minecraft.getInstance().getConnection() == null) {
                return;
            }
            try {
                var line = MicManager.getLine();
                if (line == null) {
                    LevelGatherer.updateRaw(null);
                    braek();
                    return;
                }
                var audio = createBuffer();
                var bytesRead = line.read(audio, 0, audio.length);
                if (bytesRead == 0) {
                    LevelGatherer.updateRaw(null);
                    braek();
                    return;
                }
                LevelGatherer.updateRaw(audio);
                audio = WebRTCHelper.process(NvidiaHelper.process(audio));
                if (audio == null) {
                    braek();
                    return;
                }
                if (!checkThreshold(audio)) {
                    braek();
                    return;
                }
                if (ChannelClientConfig.get().listen) {
                    byte[] finalAudio = audio;
                    Minecraft.getInstance().execute(() -> SimplePlayer.play(finalAudio, MicManager.getLine().getFormat()));
                }
                int sampleRate = MicManager.getSampleRate();
                var serialized = OpusHelper.encode(audio, sampleRate);
                NetworkHelper.sendToServer(new AudioToServerPacket(sampleRate, serialized));
            } catch (Throwable t) {
                LogUtils.getLogger().error("{}", t.toString());
            }
        }
    }

    private static boolean checkThreshold(byte[] audio) {
        var level = LevelGatherer.updateProcessed(audio);
        if (level == 0) {
            return false;
        }
        var cfg = ChannelClientConfig.get();
        if (cfg.trigger == Trigger.THRESHOLD) {
            slidingWindow.update(AudioHelper.s2dbfs(level) >= cfg.triggerThresholdDBFS);
            return slidingWindow.getSmoothedValue();
        }
        return true;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void braek() {
        LevelGatherer.updateProcessed(null);
        if (ChannelClientConfig.get().listen) {
            Minecraft.getInstance().execute(SimplePlayer::flush);
        }
    }

    private static byte[] createBuffer() {
        var format = MicManager.getLine().getFormat();
        var cfg = ChannelClientConfig.get();
        int size = (int) (frameLength * format.getFrameSize() * format.getFrameRate() / 1000);
        return new byte[size];
    }

    public static int getFrameLength() {
        return frameLength;
    }
}
