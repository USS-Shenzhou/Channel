package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.channel.gui.OutputConfigPanel;
import cn.ussshenzhou.channel.util.AudioHelper;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * @author USS_Shenzhou
 */
public class DirectAudio extends Audio {
    private final MpscArrayQueue<short[]> audioBuffer = new MpscArrayQueue<>((int) (1.1 * MAX_BUFFER_10MS));
    public final UUID playerId;

    public DirectAudio(UUID playerId) {
        super();
        this.playerId = playerId;
    }

    public void push(short[] audio) {
        int length = sampleRate / 100;
        for (int i = 0; i < audio.length / length; i++) {
            audioBuffer.offer(Arrays.copyOfRange(audio, i * length, (i + 1) * length));
        }
    }

    @Override
    @Nullable
    public List<ByteBuffer> read(int sizeIn10Ms) {
        if (audioBuffer.isEmpty()) {
            return null;
        }
        checkTooMuchDelay(audioBuffer);
        int toRead = Math.min(sizeIn10Ms, audioBuffer.size());
        int length = sampleRate / 100;
        List<ByteBuffer> buffers = new ArrayList<>(toRead);
        for (int i = 0; i < toRead; i++) {
            var chunk = audioBuffer.poll();
            if (chunk != null) {
                var buffer = ByteBuffer.allocateDirect(length * 2).order(ByteOrder.LITTLE_ENDIAN);
                buffer.asShortBuffer().put(chunk).rewind();
                buffers.add(buffer);
            }
        }
        return buffers;
    }

    @Override
    protected float getGain() {
        var vol = ChannelPlayerConfig.getOrDefault(playerId);
        OutputConfigPanel.PlayerVolumePanel.update(playerId);
        return AudioHelper.db2factor(vol);
    }

    @Override
    public Vec3 getPos(Level level) {
        var player = level.getPlayerByUUID(this.playerId);
        if (player == null) {
            return new Vec3(x, y, z);
        }
        return player.getEyePosition();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DirectAudio that) {
            return playerId.equals(that.playerId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}
