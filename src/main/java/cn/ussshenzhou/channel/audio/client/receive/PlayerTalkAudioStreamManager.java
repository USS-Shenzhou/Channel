package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.network.AudioToClientPacket;
import cn.ussshenzhou.channel.util.DumbAudioStream;
import cn.ussshenzhou.channel.util.OpusHelper;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.OpusException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PlayerTalkAudioStreamManager {
    private static final ConcurrentHashMap<Integer, SimpleAudioStream> PLAYER_STREAMS = new ConcurrentHashMap<>();

    public static void handle(AudioToClientPacket packet) {
        try {
            var decoded = OpusHelper.decode(packet.opus, packet.sampleRate);
            var from = Minecraft.getInstance().level.getEntity(packet.from);
            if (from instanceof Player player) {
                PLAYER_STREAMS.computeIfAbsent(packet.from, _ -> {
                    Minecraft.getInstance().getSoundManager().play(new PlayerTalkSoundInstance(player));
                    return new SimpleAudioStream(packet.sampleRate);
                }).enqueue(decoded);
            }
        } catch (OpusException e) {
            LogUtils.getLogger().error(e.toString());
        }
    }

    public static AudioStream get(Player player) {
        var result = PLAYER_STREAMS.get(player.getId());
        if (result == null) {
            return DumbAudioStream.INSTANCE;
        }
        return result;
    }

    public static void remove(int player) {
        var stream = PLAYER_STREAMS.remove(player);
        if (stream != null) {
            stream.close();
        }
    }

    public static class SimpleAudioStream implements AudioStream {
        private final AudioFormat format;
        private final BlockingQueue<short[]> queue = new LinkedBlockingQueue<>();
        private volatile boolean closed = false;

        public SimpleAudioStream(int sampleRate) {
            this.format = new AudioFormat(sampleRate, 16, 1, true, false);
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public ByteBuffer read(int wanted) {
            if (closed) {
                return ByteBuffer.allocate(0);
            }
            short[] audio = queue.poll();
            if (audio == null) {
                return ByteBuffer.allocate(0);
            }
            ByteBuffer buf = ByteBuffer.allocateDirect(audio.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            buf.asShortBuffer().put(audio);
            buf.limit(audio.length * 2);
            return buf;
        }

        @Override
        public void close() {
            closed = true;
            queue.clear();
        }

        public void enqueue(short[] pcm) {
            if (!closed) {
                //noinspection ResultOfMethodCallIgnored
                queue.offer(pcm);
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }

}
