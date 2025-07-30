package cn.ussshenzhou.channel.audio.server;

import cn.ussshenzhou.channel.network.AudioToClientPacket;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author USS_Shenzhou
 */
public class RelayHandler {

    public static void process(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        normalTalking(from, opusAudio, sampleRate);
    }

    public static void normalTalking(ServerPlayer from, byte[] opusAudio, int sampleRate) {
        from.level().players().stream().filter(to ->
                        //FIXME to.getId() != from.getId() &&
                                to.position().distanceTo(from.position()) < 24 &&
                                (!from.isSpectator() || to.isSpectator())
                )
                .forEach(to -> NetworkHelper.sendToPlayer(to, new AudioToClientPacket(sampleRate, from.getId(), opusAudio)));
    }
}
