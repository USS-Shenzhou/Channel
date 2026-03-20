package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.client.rt.RayTraceManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.EXTEfx.*;

/**
 * @author USS_Shenzhou
 */
public class PlayerAudio {
    private static final int MAX_BUFFER = 500;
    private static final int MIN_PLAY_THRESHOLD = 51;
    private final BlockingQueue<short[]> audioBuffer = new ArrayBlockingQueue<>(MAX_BUFFER);
    private int readyBufferMs = 0;
    public final int alSource, sampleRate, alDirectFilter, alReverbFilter;
    public final UUID playerId;

    public PlayerAudio(UUID playerId, int sampleRate) {
        this.playerId = playerId;
        this.sampleRate = sampleRate;
        this.alSource = alGenSources();
        alSourcef(alSource, AL_GAIN, 1);
        alSourcef(alSource, AL_PITCH, 1);
        alSourcef(alSource, AL_LOOPING, AL_FALSE);
        alSourcef(alSource, AL_REFERENCE_DISTANCE, 1.5f);
        alSourcef(alSource, AL_MAX_GAIN, AudioHelper.db2factor(30));
        if (ChannelClientConfig.get().rayTraceAudio) {
            alSourcef(alSource, AL_ROLLOFF_FACTOR, 0);
            alDirectFilter = alGenFilters();
            alFilteri(alDirectFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            alSourcei(alSource, AL_DIRECT_FILTER, alDirectFilter);

            alReverbFilter = alGenFilters();
            alFilteri(alReverbFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            alSource3i(alSource, AL_AUXILIARY_SEND_FILTER, RayTraceManager.getSlot(), 0, alReverbFilter);
        } else {
            alSourcef(alSource, AL_ROLLOFF_FACTOR, 1);
            alDirectFilter = -1;
            alReverbFilter = -1;
        }
    }

    public void push(short[] audio) {
        int length = sampleRate / 100;
        for (int i = 0; i < audio.length / length; i++) {
            audioBuffer.offer(Arrays.copyOfRange(audio, i * length, (i + 1) * length));
        }
    }

    @Nullable
    public List<ByteBuffer> read(int sizeIn10Ms) {
        int available = audioBuffer.size();
        if (available == 0) {
            return null;
        }
        int toRead = Math.min(sizeIn10Ms, available);
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

    public void play() {
        int processed = alGetSourcei(alSource, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buf = alSourceUnqueueBuffers(alSource);
            readyBufferMs -= 10;
            alDeleteBuffers(buf);
        }
        var pcms = read(MAX_BUFFER);
        if (pcms != null) {
            for (ByteBuffer pcm : pcms) {
                int buf = alGenBuffers();
                alBufferData(buf, AL_FORMAT_MONO16, pcm, sampleRate);
                alSourceQueueBuffers(alSource, buf);
                readyBufferMs += 10;
            }
        }
        if (alGetSourcei(alSource, AL_SOURCE_STATE) != AL_PLAYING && readyBufferMs > MIN_PLAY_THRESHOLD) {
            alSourcePlay(alSource);
        }
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
