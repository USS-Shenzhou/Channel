package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.client.rt.RayTraceManager;
import cn.ussshenzhou.channel.audio.client.rt.SourceAudioData;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL10.AL_FALSE;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_MAX_GAIN;
import static org.lwjgl.openal.AL10.AL_PITCH;
import static org.lwjgl.openal.AL10.AL_REFERENCE_DISTANCE;
import static org.lwjgl.openal.AL10.AL_ROLLOFF_FACTOR;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.AL11.alSource3i;
import static org.lwjgl.openal.EXTEfx.*;
import static org.lwjgl.openal.EXTEfx.AL_AUXILIARY_SEND_FILTER;

public abstract class Audio {
    protected static final int MAX_BUFFER_10MS = 3 * 100;
    protected int readyBufferMs = 0;
    protected final int alSource, sampleRate, alDirectFilter, alReverbFilter;
    protected double x, y, z;

    public Audio(int sampleRate) {
        this.sampleRate = sampleRate;
        this.alSource = alGenSources();
        alSourcef(alSource, AL_GAIN, 1);
        alSourcef(alSource, AL_PITCH, 1);
        alSourcef(alSource, AL_LOOPING, AL_FALSE);
        alSourcef(alSource, AL_REFERENCE_DISTANCE, 2);
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

    public void updateSourceParameters(SourceAudioData data, int auxSlot) {
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAIN, data.directGain());
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAINHF, data.directHF());
        alSourcei(this.alSource, AL_DIRECT_FILTER, this.alDirectFilter);

        alFilterf(this.alReverbFilter, AL_LOWPASS_GAIN, data.reverbGain());
        alFilterf(this.alReverbFilter, AL_LOWPASS_GAINHF, 1);
        alSource3i(this.alSource, AL_AUXILIARY_SEND_FILTER, auxSlot, 0, this.alReverbFilter);

        alSource3f(this.alSource, AL_POSITION, (float) data.virtualPos().x, (float) data.virtualPos().y, (float) data.virtualPos().z);
    }

    public boolean play() {
        //TODO close self by last active time
        //TODO use low-pass filter to make underwater effect in 26.2
        alSourcef(alSource, AL_GAIN, getGain());
        int processed = alGetSourcei(alSource, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buf = alSourceUnqueueBuffers(alSource);
            readyBufferMs -= 10;
            alDeleteBuffers(buf);
        }
        var pcms = read(MAX_BUFFER_10MS);
        if (pcms != null) {
            for (ByteBuffer pcm : pcms) {
                int buf = alGenBuffers();
                alBufferData(buf, AL_FORMAT_MONO16, pcm, sampleRate);
                alSourceQueueBuffers(alSource, buf);
                readyBufferMs += 10;
            }
        }

        int state = alGetSourcei(alSource, AL_SOURCE_STATE);
        if (state != AL_PLAYING) {
            int threshold = (state == AL_INITIAL) ? ChannelClientConfig.get().networkTolerance : ChannelClientConfig.get().networkTolerance / 5;
            if (readyBufferMs > threshold) {
                alSourcePlay(alSource);
            }
        }
        return false;
    }

    protected abstract float getGain();

    @Nullable
    public abstract List<ByteBuffer> read(int sizeIn10Ms);

    protected static void checkTooMuchDelay(MpscArrayQueue<short[]> buffer) {
        if (buffer.size() >= ChannelClientConfig.get().networkTolerance * 1.5 / 10 || buffer.size() >= MAX_BUFFER_10MS) {
            int targetBufferSize = (int) (ChannelClientConfig.get().networkTolerance * 1.1 / 10);
            int drop = buffer.size() - targetBufferSize;
            for (int i = 0; i < drop; i++) {
                buffer.poll();
            }
        }
    }

    public void close() {
        alSourceStop(this.alSource);
        int alBuf = alGetSourcei(this.alSource, AL_BUFFERS_QUEUED);
        while (alBuf-- > 0) {
            alDeleteBuffers(alSourceUnqueueBuffers(this.alSource));
        }
        alDeleteSources(this.alSource);
        alDeleteFilters(this.alDirectFilter);
        alDeleteFilters(this.alReverbFilter);
    }

    public Vec3 getPos(Level level) {
        return new Vec3(x, y, z);
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
