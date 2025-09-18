package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.network.BaseAudioPacket2C;
import cn.ussshenzhou.channel.network.TalkPacket2C;

/**
 * @author USS_Shenzhou
 */
public class AudioManagerManager {
    public static final TalkManager TALK = new TalkManager();

    public static void init() {
        TALK.init();
    }

    public static void handlePacket(BaseAudioPacket2C packet) {
        switch (packet) {
            case TalkPacket2C talk -> TALK.handle(packet.from, packet.sampleRate, packet.opus);
            //TODO case SpeakerPacket2C speaker -> SPEAKER.handle(packet.from, packet.sampleRate, packet.opus);
            default -> {
            }
        }
    }
}
