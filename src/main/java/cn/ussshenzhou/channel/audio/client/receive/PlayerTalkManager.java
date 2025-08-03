package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.network.AudioToClientPacket;
import cn.ussshenzhou.channel.util.OpusHelper;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.OpusException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class PlayerTalkManager {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final ConcurrentHashMap<Integer, PlayerAudio> PLAYER_AUDIOS = new ConcurrentHashMap<>();
    private static final int BUFFER_LENGTH = 5;
    private static long alCtx, alDevice;

    public static void init() {
        SCHEDULER.submit(() -> {
            while (!Minecraft.getInstance().getSoundManager().soundEngine.loaded) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            }
            initAL();
            alDistanceModel(AL_EXPONENT_DISTANCE);
        });
        SCHEDULER.scheduleAtFixedRate(PlayerTalkManager::playing, 0, BUFFER_LENGTH * 10, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    private static void initAL() {
        var library = Minecraft.getInstance().getSoundManager().soundEngine.library;
        alCtx = library.context;
        alDevice = library.currentDevice;
        alcMakeContextCurrent(alCtx);
        AL.createCapabilities(ALC.createCapabilities(alDevice));
    }

    public static void handlePacket(AudioToClientPacket packet) {
        try {
            var decoded = OpusHelper.decode(packet.opus, packet.sampleRate);
            //Minecraft.getInstance().execute(() -> SimplePlayer.play(ArrayHelper.reinterpretS2B(decoded), new AudioFormat(16000, 16, 1, true, false)));
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var from = level.getEntity(packet.from);
            if (from instanceof Player player) {
                PLAYER_AUDIOS.compute(player.getId(), (id, old) -> {
                    if (old == null) {
                        return new PlayerAudio(id, packet.sampleRate);
                    } else if (old.sampleRate != packet.sampleRate) {
                        old.close();
                        return new PlayerAudio(id, packet.sampleRate);
                    } else {
                        return old;
                    }
                }).push(decoded);
            }
        } catch (OpusException e) {
            LogUtils.getLogger().error(e.toString());
        }
    }

    private static void playing() {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                PLAYER_AUDIOS.values().forEach(PlayerAudio::close);
                PLAYER_AUDIOS.clear();
                return;
            }
            var library = Minecraft.getInstance().getSoundManager().soundEngine.library;
            if (library.context != alCtx || library.currentDevice != alDevice) {
                PLAYER_AUDIOS.values().forEach(PlayerAudio::close);
                PLAYER_AUDIOS.clear();
                initAL();
            }
            var it = PLAYER_AUDIOS.entrySet().iterator();
            var partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            while (it.hasNext()) {
                var entry = it.next();
                var player = level.getEntity(entry.getKey());
                var audio = entry.getValue();
                if (player == null) {
                    audio.close();
                    it.remove();
                    continue;
                }
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
            }
        } catch (Throwable e) {
            LogUtils.getLogger().error(e.toString());
        }

    }

}
