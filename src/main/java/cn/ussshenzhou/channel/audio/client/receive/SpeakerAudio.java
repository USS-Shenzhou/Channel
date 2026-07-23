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

public class SpeakerAudio extends BaseSharedAudio {
    private final WeakReference<SpeakerBlockEntity> speakerBlockEntity;
    protected int x, y, z;
    public static final float REVERB_ENHANCE = 3f;

    public SpeakerAudio(SpeakerBlockEntity speakerBlockEntity) {
        super();
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

    @Override
    public void updateSourceParameters(SourceAudioData data, int auxSlot) {
        var ear = AudioHelper.getEarPos();
        double correctedGain = Math.pow(data.directGain(), 0.1);
        double distance = Math.sqrt(ear.distanceToSqr(x, y, z));
        double extraDecay = 1;
        if (distance >= 32) {
            extraDecay = 1 - (distance - 32) / 32;
        }
        double directWallDecay = Math.pow(0.5, data.wallThickness());
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAIN, (float) (extraDecay * correctedGain * directWallDecay));
        alFilterf(this.alDirectFilter, AL_LOWPASS_GAINHF, data.directHF());
        alSourcei(this.alSource, AL_DIRECT_FILTER, this.alDirectFilter);

        alFilterf(this.alReverbFilter, AL_LOWPASS_GAIN, (float) (Math.sqrt(data.reverbGain()) * REVERB_ENHANCE));
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
