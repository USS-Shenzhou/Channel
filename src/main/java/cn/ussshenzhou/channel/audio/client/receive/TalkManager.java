package cn.ussshenzhou.channel.audio.client.receive;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL10.AL_BUFFERS_PROCESSED;
import static org.lwjgl.openal.AL10.AL_BUFFERS_QUEUED;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceQueueBuffers;
import static org.lwjgl.openal.AL10.alSourceUnqueueBuffers;

/**
 * @author USS_Shenzhou
 */
public class TalkManager extends BaseAudioManager {
    @Override
    protected boolean play(Level level, int playerId, PlayerAudio audio) {
        var player = level.getEntity(playerId);
        if (player == null) {
            audio.close();
            return true;
        }
        var partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var pos = player.getEyePosition(partialTick);
        alSource3f(audio.alSource, AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        int processed = alGetSourcei(audio.alSource, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buf = alSourceUnqueueBuffers(audio.alSource);
            alDeleteBuffers(buf);
        }
        ByteBuffer pcm = audio.read(BUFFER_LENGTH);
        int buf = alGenBuffers();
        alBufferData(buf, AL_FORMAT_MONO16, pcm, audio.sampleRate);
        alSourceQueueBuffers(audio.alSource, buf);
        int state = alGetSourcei(audio.alSource, AL_SOURCE_STATE);
        if (state != AL_PLAYING && alGetSourcei(audio.alSource, AL_BUFFERS_QUEUED) > 0) {
            alSourcePlay(audio.alSource);
        }
        return false;
    }
}
