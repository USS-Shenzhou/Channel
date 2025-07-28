package cn.ussshenzhou.channel.audio.client;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.network.AudioToServerPacket;
import cn.ussshenzhou.channel.util.ArrayHelper;
import cn.ussshenzhou.channel.util.OpusHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.OpusException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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
    private static ScheduledFuture<?> keepReading;

    @SubscribeEvent
    public static void startReading(FMLClientSetupEvent event) {
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
    }

    public static void frameLengthChange() {
        keepReading.cancel(false);
        keepReading = SCHEDULER.scheduleAtFixedRate(MicReader::read, 0, ChannelClientConfig.get().frameLengthMs, TimeUnit.MILLISECONDS);
    }

    private static void read() {
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
            short[] toEncode;
            if (ChannelClientConfig.get().noiseCanceling) {
                toEncode = NoiseCanceller.process(raw);
            } else {
                toEncode = ArrayHelper.reinterpretB2S(raw);
            }
            var format = MicManager.getLine().getFormat();
            var serialized = OpusHelper.encode(toEncode, (int) format.getSampleRate());
            //DebugSimplePlayer.play(ArrayHelper.reinterpretS2B(toEncode), format);
            NetworkHelper.sendToServer(new AudioToServerPacket((int) format.getSampleRate(), serialized));
        } catch (OpusException e) {
            LogUtils.getLogger().error("{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Throwable t) {
            LogUtils.getLogger().error("{}", t.getMessage());
            ChannelClientConfig.write(c -> c.noiseCanceling = false);
        }
    }

    private static byte[] createBuffer() {
        var format = MicManager.getLine().getFormat();
        var cfg = ChannelClientConfig.get();
        int size = (int) (cfg.frameLengthMs * format.getFrameSize() * format.getFrameRate() / 1000);
        return new byte[size];
    }
}
