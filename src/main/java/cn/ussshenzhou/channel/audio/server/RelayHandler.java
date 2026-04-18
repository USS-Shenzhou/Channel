package cn.ussshenzhou.channel.audio.server;

import cn.ussshenzhou.channel.network.AudioPacket2C;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author USS_Shenzhou
 */
public class RelayHandler {

    public static final ExecutorService SERVER_THREAD = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Server-Thread-%d")
            .setDaemon(true)
            .build());

    public static void process(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        SERVER_THREAD.execute(() -> {
            normalTalking(from, opusAudio, sampleRate);
        });
    }

    public static void normalTalking(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        from.level().players().stream().filter(to ->
                        (SharedConstants.IS_RUNNING_IN_IDE || to.getId() != from.getId()) &&
                                to.position().distanceTo(from.position()) < 64 &&
                                (!from.isSpectator() || to.isSpectator())
                )
                .forEach(to -> NetworkHelper.sendToPlayer(to, new AudioPacket2C(sampleRate, from.getUUID(), opusAudio)));
    }
}
