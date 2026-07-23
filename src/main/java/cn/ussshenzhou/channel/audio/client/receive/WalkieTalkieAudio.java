package cn.ussshenzhou.channel.audio.client.receive;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.openal.AL10.*;

public class WalkieTalkieAudio extends BaseSharedAudio {

    @Override
    protected void initAL() {
        alSourcef(alSource, AL_GAIN, 1.0f);
        alSourcef(alSource, AL_PITCH, 1.0f);
        alSourcei(alSource, AL_LOOPING, AL_FALSE);
        alSourcei(alSource, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(alSource, AL_POSITION, 0.0f, 0.0f, 0.0f);
        alSource3f(alSource, AL_VELOCITY, 0.0f, 0.0f, 0.0f);
        alSourcef(alSource, AL_ROLLOFF_FACTOR, 0.0f);
        alDirectFilter = -1;
        alReverbFilter = -1;
    }

    @Override
    public Vec3 getPos(Level level) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            this.close();
            return new Vec3(0, 0, 0);
        }
        return player.getEyePosition();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}
