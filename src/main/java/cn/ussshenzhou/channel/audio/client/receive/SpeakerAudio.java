package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.client.rt.SourceAudioData;
import cn.ussshenzhou.channel.blockentity.SpeakerBlockEntity;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.alSource3i;
import static org.lwjgl.openal.EXTEfx.*;

public class SpeakerAudio extends Audio {
    private final WeakReference<SpeakerBlockEntity> speakerBlockEntity;
    private final ConcurrentHashMap<UUID, MpscArrayQueue<short[]>> audioBuffers = new ConcurrentHashMap<>();
    protected int x, y, z;
    public static final float REVERB_ENHANCE = 1.5f;

    public SpeakerAudio(SpeakerBlockEntity speakerBlockEntity, int sampleRate) {
        super(sampleRate);
        this.speakerBlockEntity = new WeakReference<>(speakerBlockEntity);
        var pos = speakerBlockEntity.getBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        super.setPos(x, y, z);
        if (!ChannelClientConfig.get().rayTraceAudio) {
            alSourcef(alSource, AL_ROLLOFF_FACTOR, 0.2f);
        }
    }

    public void push(UUID from, short[] audio) {
        int length = sampleRate / 100;
        var queue = audioBuffers.computeIfAbsent(from, _ -> new MpscArrayQueue<>((int) (1.1 * MAX_BUFFER_10MS)));
        for (int i = 0; i < audio.length / length; i++) {
            queue.offer(Arrays.copyOfRange(audio, i * length, (i + 1) * length));
        }
    }

    @Override
    public void updateSourceParameters(SourceAudioData data, int auxSlot) {
        var ear = AudioHelper.getEarPos();
        double correctedGain = Math.pow(data.directGain(), 0.1);
        double distance = Math.sqrt(ear.distanceToSqr(x, y, z));
        double extraDecay = 1;
        if (distance >= 32) {
            extraDecay = 1 - (distance - 32) / 32;
        }
        double wallDecay = Math.pow(0.5, data.wallThickness());
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAIN, (float) (extraDecay * correctedGain * wallDecay / REVERB_ENHANCE));
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAINHF, data.directHF());
        alSourcei(this.alSource, AL_DIRECT_FILTER, this.alDirectFilter);

        alFilterf(this.alReverbFilter, AL_LOWPASS_GAIN, (float) (extraDecay * wallDecay));
        alFilterf(this.alReverbFilter, AL_LOWPASS_GAINHF, 1);
        alSource3i(this.alSource, AL_AUXILIARY_SEND_FILTER, auxSlot, 0, this.alReverbFilter);

        alSource3f(this.alSource, AL_POSITION, (float) data.virtualPos().x, (float) data.virtualPos().y, (float) data.virtualPos().z);
    }

    @Override
    public boolean play() {
        var entity = speakerBlockEntity.get();
        if (entity == null || Minecraft.getInstance().level == null || Minecraft.getInstance().level.getBlockEntity(entity.getBlockPos()) != entity) {
            this.close();
            return true;
        }
        return super.play();
    }

    @Override
    protected float getGain() {
        return REVERB_ENHANCE * AudioHelper.db2factor(ChannelClientConfig.get().outputAdjust);
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

    @Override
    public int hashCode() {
        return (y + z * 31) * 71 + x;
    }

    public static int hashcode(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 31) * 71 + pos.getX();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpeakerAudio that) {
            return that.x == this.x && that.y == this.y && that.z == this.z;
        }
        return false;
    }
}
