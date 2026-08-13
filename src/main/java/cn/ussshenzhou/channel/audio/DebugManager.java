package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import cn.ussshenzhou.channel.util.IntervalCounter;
import cn.ussshenzhou.channel.util.TimeCounter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.EffectiveSide;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DebugManager {
    public static final int MEASURE_WINDOW_MS = 2000;
    public static final int LONG_MEASURE_WINDOW_MS = 5000;
    public static final IntervalCounter MIC_SEND_COUNTER = new IntervalCounter(ChannelClientConfig.get().frameLengthMs, MEASURE_WINDOW_MS);
    public static final IntervalCounter PLAY_COUNTER = new IntervalCounter(10, MEASURE_WINDOW_MS);
    public static final HashMap<UUID, IntervalCounter> RECEIVE_COUNTER = new HashMap<>();
    public static final TimeCounter PLAY_RESET_COUNTER = new TimeCounter(LONG_MEASURE_WINDOW_MS);
    public static final TimeCounter OPENAL_REPLAY_COUNTER = new TimeCounter(LONG_MEASURE_WINDOW_MS);
    public static final TimeCounter ICMP_PING = new TimeCounter(LONG_MEASURE_WINDOW_MS);
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Mic-Debug-Thread-%d")
            .setDaemon(true)
            .build());
    private static final Int2LongOpenHashMap OPUS_SEND_CACHE = new Int2LongOpenHashMap();
    public static final TimeCounter RELAY_PING = new TimeCounter(LONG_MEASURE_WINDOW_MS);

    static {
        if (EffectiveSide.get().isClient()) {
            SCHEDULER.scheduleAtFixedRate(DebugManager::ping, 0, 500, TimeUnit.MILLISECONDS);
        }
    }

    public static void refresh() {
        MIC_SEND_COUNTER.setIdealIntervalMs(ChannelClientConfig.get().frameLengthMs);
        PLAY_COUNTER.reset();
        RECEIVE_COUNTER.clear();
    }

    public static void sending(byte[] opus) {
        OPUS_SEND_CACHE.put(Arrays.hashCode(opus), Util.getNanos());
    }

    public static void receiving(byte[] opus) {
        var hashcode = Arrays.hashCode(opus);
        if (OPUS_SEND_CACHE.containsKey(hashcode)) {
            RELAY_PING.put((int) (Util.getNanos() - OPUS_SEND_CACHE.get(hashcode)) / 1000);
            OPUS_SEND_CACHE.remove(hashcode);
        }
        if (OPUS_SEND_CACHE.size() > 500) {
            OPUS_SEND_CACHE.clear();
        }
    }

    private static void ping() {
        Thread.startVirtualThread(() -> {
            String host = null;
            if (SubspaceConnection.using() && SubspaceConnection.getChannel().remoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                host = inetSocketAddress.getHostString();
            } else {
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    var con = connection.getConnection();
                    if (con.isConnected() && con.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                        host = inetSocketAddress.getHostString();
                    }
                }
            }
            if (host == null) {
                return;
            }
            long start = Util.getNanos();
            try {
                boolean reachable = InetAddress.getByName(host).isReachable(LONG_MEASURE_WINDOW_MS);
                if (reachable) {
                    ICMP_PING.put((int) ((Util.getNanos() - start) / 1000));
                } else {
                    ICMP_PING.put(LONG_MEASURE_WINDOW_MS);
                }
            } catch (IOException ignored) {
            }
        });
    }


    //TODO leave world clear
}
