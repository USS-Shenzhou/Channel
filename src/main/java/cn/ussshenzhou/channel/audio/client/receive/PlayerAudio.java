package cn.ussshenzhou.channel.audio.client.receive;

import org.lwjgl.openal.AL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.lwjgl.openal.AL10.*;

/**
 * @author USS_Shenzhou
 */
public class PlayerAudio {
    private final BlockingQueue<short[]> audioBuffer = new ArrayBlockingQueue<>(20);
    protected final int playerId, alSource, sampleRate;

    public PlayerAudio(int playerId, int sampleRate) {
        this.playerId = playerId;
        this.sampleRate = sampleRate;
        this.alSource = alGenSources();
        alSourcef(alSource, AL_GAIN, 1);
        alSourcef(alSource, AL_PITCH, 1);
        alSourcef(alSource, AL_LOOPING, AL_FALSE);
        alSourcef(alSource, AL_SOURCE_TYPE, AL11.AL_STREAMING);
        alSourcef(alSource, AL_REFERENCE_DISTANCE, 3);
        alSourcef(alSource, AL_ROLLOFF_FACTOR, 1);
    }

    public void push(short[] audio) {
        int length = sampleRate / 100;
        for (int i = 0; i < audio.length / length; i++) {
            audioBuffer.offer(Arrays.copyOfRange(audio, i * length, (i + 1) * length));
        }
    }

    public ByteBuffer read(int sizeIn10Ms) {
        int length = sampleRate / 100;
        var buffer = ByteBuffer.allocateDirect(sizeIn10Ms * length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sizeIn10Ms; i++) {
            if (i < sizeIn10Ms - audioBuffer.size()) {
                continue;
            }
            buffer.slice(i * length * 2, length * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audioBuffer.poll());
        }
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public void close() {
        alSourceStop(this.alSource);
        int alBuf = alGetSourcei(this.alSource, AL_BUFFERS_QUEUED);
        while (alBuf-- > 0) {
            alDeleteBuffers(alSourceUnqueueBuffers(alBuf));
        }
        alDeleteSources(this.alSource);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PlayerAudio that)) {
            return false;
        }
        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playerId);
    }
}
