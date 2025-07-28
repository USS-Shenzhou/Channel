package cn.ussshenzhou.channel.audio.server;

import cn.ussshenzhou.channel.network.AudioToClientPacket;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import static net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer;

/**
 * @author USS_Shenzhou
 */
public class RelayHandler {

    public static void process(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        normalTalking(from, opusAudio, sampleRate);
    }

    public static void normalTalking(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        from.level().players().stream().filter(to ->
                        to.position().distanceTo(from.position()) < 24 &&
                                (!from.isSpectator() || to.isSpectator())
                )
                .forEach(to -> NetworkHelper.sendToPlayer(to, new AudioToClientPacket(sampleRate, from.getId(), opusAudio)));
    }
}
