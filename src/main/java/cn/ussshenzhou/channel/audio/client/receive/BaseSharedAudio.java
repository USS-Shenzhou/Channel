package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseSharedAudio extends Audio {
    protected final ConcurrentHashMap<UUID, MpscArrayQueue<short[]>> audioBuffers = new ConcurrentHashMap<>();

    public void push(UUID from, short[] audio) {
        int length = sampleRate / 100;
        var queue = audioBuffers.computeIfAbsent(from, _ -> new MpscArrayQueue<>((int) (1.1 * MAX_BUFFER_10MS)));
        for (int i = 0; i < audio.length / length; i++) {
            queue.offer(Arrays.copyOfRange(audio, i * length, (i + 1) * length));
        }
    }

    @Override
    protected float getGain() {
        return AudioHelper.db2factor(ChannelClientConfig.get().outputAdjust);
    }

    @Override
    @Nullable
    public List<ByteBuffer> read(int sizeIn10Ms) {
        if (audioBuffers.isEmpty()) {
            return null;
        }
        audioBuffers.entrySet().removeIf(e -> e.getValue().isEmpty());
        checkTooMuchDelay();
        int maxAvailable = 0;
        for (var queue : audioBuffers.values()) {
            maxAvailable = Math.max(maxAvailable, queue.size());
        }
        if (maxAvailable == 0) {
            return null;
        }
        int toRead = Math.min(sizeIn10Ms, maxAvailable);
        int length = sampleRate / 100;
        List<ByteBuffer> buffers = new ArrayList<>(toRead);
        for (int i = 0; i < toRead; i++) {
            var mixed = mix(length);
            if (mixed != null) {
                buffers.add(write(mixed));
            }
        }
        return buffers;
    }

    private int[] mix(int length) {
        int[] mixed = new int[length];
        boolean hasData = false;
        for (var queue : audioBuffers.values()) {
            var chunk = queue.poll();
            if (chunk != null) {
                hasData = true;
                for (int j = 0; j < length; j++) {
                    mixed[j] += chunk[j];
                }
            }
        }
        return hasData ? mixed : null;
    }

    private ByteBuffer write(int[] mixed) {
        var buffer = ByteBuffer.allocateDirect(mixed.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        var shortBuffer = buffer.asShortBuffer();
        for (int i : mixed) {
            shortBuffer.put((short) (Short.MAX_VALUE * Math.tanh((double) i / Short.MAX_VALUE)));
        }
        buffer.rewind();
        return buffer;
    }

    private void checkTooMuchDelay() {
        for (var queue : audioBuffers.values()) {
            checkTooMuchDelay(queue);
        }
    }
}
