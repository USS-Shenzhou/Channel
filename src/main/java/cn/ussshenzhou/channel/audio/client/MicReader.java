package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.ArrayHelper;
import cn.ussshenzhou.channel.util.AudioHelper;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class MicReader {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> task;
    private static ByteBuffer buffer;

    @SubscribeEvent
    public static void startReading(FMLClientSetupEvent event) {
        task = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
    }

    public static void frameLengthChange() {
        task.cancel(false);
        task = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
    }

    private static void read() {
        var line = MicManager.getLine();
        if (line == null) {
            return;
        }
        var raw = createBuffer();
        var bytesRead = line.read(raw, 0, raw.length);
        if (bytesRead == 0) {
            return;
        }
        short[] toEncode = new short[0];
        try {
            if (ChannelClientConfig.get().noiseCanceling) {
                toEncode = NoiseCanceller.process(raw);
            } else {
                toEncode = ArrayHelper.reinterpretB2S(raw);
            }
        } catch (Throwable t) {
            LogUtils.getLogger().error("{}", t.getMessage());
            ChannelClientConfig.write(c -> c.noiseCanceling = false);
        }
        DebugSimplePlayer.play(ArrayHelper.reinterpretS2B(toEncode), MicManager.getLine().getFormat());
    }

    private static byte[] createBuffer() {
        var format = MicManager.getLine().getFormat();
        var cfg = ChannelClientConfig.get();
        int size = (int) (cfg.frameLengthMs * format.getFrameSize() * format.getFrameRate() / 1000);
        return new byte[size];
    }
}
