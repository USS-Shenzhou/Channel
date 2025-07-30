package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.network.AudioToServerPacket;
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

    public static void init() {
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
    }

    public static void frameLengthChange() {
        synchronized (MicReader.class) {
            keepReading.cancel(false);
            keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
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
                    return;
                }
                var raw = createBuffer();
                var bytesRead = line.read(raw, 0, raw.length);
                if (bytesRead == 0) {
                    return;
                }
                var processed = WebRTCHelper.process(raw);
                if (ChannelClientConfig.get().listen) {
                    Minecraft.getInstance().execute(() -> SimplePlayer.play(processed, MicManager.getLine().getFormat()));
                }
                if (processed == null) {
                    return;
                }
                int sampleRate = (int) ChannelClientConfig.get().sampleRate;
                var serialized = OpusHelper.encode(processed, sampleRate);
                NetworkHelper.sendToServer(new AudioToServerPacket(sampleRate, serialized));
            } catch (Throwable t) {
                LogUtils.getLogger().error("{}", t.toString());
            }
        }
    }

    private static byte[] createBuffer() {
        var format = MicManager.getLine().getFormat();
        var cfg = ChannelClientConfig.get();
        int size = (int) (cfg.frameLengthMs * format.getFrameSize() * format.getFrameRate() / 1000);
        return new byte[size];
    }
}
