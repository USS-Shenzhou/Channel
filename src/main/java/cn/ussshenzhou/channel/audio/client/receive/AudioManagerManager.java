package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.network.AudioPacket2C;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author USS_Shenzhou
 */
public class AudioManagerManager {
    public static final ExecutorService HANDLER_THREAD = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Audio-Data-Handler-%d")
            .setDaemon(true)
            .build());

    public static final TalkManager TALK = new TalkManager();

    public static void init() {
        TALK.init();
    }

    public static void reset() {
        TALK.reset();
    }

    public static void handlePacket(AudioPacket2C packet) {
        TALK.handle(packet);
        //TODO case SpeakerPacket2C speaker -> SPEAKER.handle(packet.from, packet.sampleRate, packet.opus);
    }
}
