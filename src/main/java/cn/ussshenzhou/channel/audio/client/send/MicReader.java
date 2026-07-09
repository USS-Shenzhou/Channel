package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.hud.MicrophoneHud;
import cn.ussshenzhou.channel.network.TalkPacket2S;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.channel.audio.OpusManager;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Mic-Reader-Thread-%d")
            .setDaemon(true)
            .build());
    private static ScheduledFuture<?> keepReading;
    private static WebRTCHelper.SimpleSlidingBooleanWindow slidingWindow = null;
    private static int frameLength = 0;

    public static void init() {
        frameLength = ChannelClientConfig.get().frameLengthMs;
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, frameLength, TimeUnit.MILLISECONDS);
        slidingWindow = new WebRTCHelper.SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / frameLength);
    }

    public static synchronized void frameLengthChange() {
        keepReading.cancel(false);
        frameLength = ChannelClientConfig.get().frameLengthMs;
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, frameLength, TimeUnit.MILLISECONDS);
        slidingWindow = new WebRTCHelper.SimpleSlidingBooleanWindow(ModConstant.VAD_SMOOTH_WINDOW_LENGTH_MS / frameLength, slidingWindow);
    }

    private static synchronized void read() {
        if (Minecraft.getInstance().getConnection() == null || MicrophoneHud.getStatus() == MicrophoneHud.Status.SUBSPACE || MicrophoneHud.getStatus() == MicrophoneHud.Status.OP) {
            return;
        }
        var cfg = ChannelClientConfig.get();
        if ((cfg.trigger == Trigger.PUSH_TO_TALK || cfg.trigger == Trigger.SWITCH) && !cfg.onAir) {
            MicrophoneHud.setStatus(MicrophoneHud.Status.MUTE);
            return;
        }
        try {
            var line = MicManager.getLine();
            if (line == null) {
                LevelGatherer.updateRaw(null);
                MicrophoneHud.setStatus(MicrophoneHud.Status.ERROR);
                braek();
                return;
            }
            //-----raw data from mic-----
            var audio = createBuffer();
            var bytesRead = line.read(audio, 0, audio.length);
            if (bytesRead == 0) {
                LevelGatherer.updateRaw(null);
                MicrophoneHud.setStatus(MicrophoneHud.Status.ERROR);
                braek();
                return;
            }
            LevelGatherer.updateRaw(audio);
            //-----pre-process-----
            int networkSampleRate = (int) ChannelClientConfig.get().networkSampleRate;
            audio = WebRTCHelper.process(NvidiaHelper.process(audio), MicManager.getSampleRate(), networkSampleRate);
            if (audio == null) {
                MicrophoneHud.setStatus(MicrophoneHud.Status.VAD_FAIL);
                braek();
                return;
            }
            if (!checkThreshold(audio)) {
                braek();
                return;
            }
            //-----playback-----
            if (ChannelClientConfig.get().listen) {
                byte[] finalAudio = audio;
                Minecraft.getInstance().execute(() -> SimplePlayer.play(finalAudio, networkSampleRate));
            }
            //-----encode-----
            var opus = OpusManager.encode(audio, networkSampleRate);
            var packet = new TalkPacket2S(opus);
            if (SubspaceConnection.using()) {
                SubspaceConnection.send(packet);
            } else {
                NetworkHelper.sendToServer(packet);
            }
            MicrophoneHud.setStatus(MicrophoneHud.Status.TALKING);
        } catch (Throwable t) {
            LogUtils.getLogger().error("{}", t.toString());
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
            var r = slidingWindow.getSmoothedValue();
            if (!r) {
                MicrophoneHud.setStatus(MicrophoneHud.Status.VOLUME_FAIL);
            }
            return r;
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
        int size = (int) (frameLength * format.getFrameSize() * format.getFrameRate() / 1000);
        return new byte[size];
    }

    public static int getFrameLength() {
        return frameLength;
    }
}
