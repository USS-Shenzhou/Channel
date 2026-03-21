package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.network.BaseAudioPacket2C;
import cn.ussshenzhou.channel.util.ArrayHelper;
import cn.ussshenzhou.channel.util.OpusHelper;
import com.mojang.logging.LogUtils;
import io.github.jaredmdobson.concentus.OpusException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.openal.AL10.alDistanceModel;
import static org.lwjgl.openal.AL11.AL_EXPONENT_DISTANCE;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;

/**
 * @author USS_Shenzhou
 */
public abstract class BaseAudioManager {
    public static final ScheduledExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r ->
            Thread.ofPlatform()
                    .name("Channel-Audio-Play-Thread")
                    .daemon(true)
                    .factory()
                    .newThread(r)
    );
    protected final ConcurrentHashMap<UUID, PlayerAudio> playerAudios = new ConcurrentHashMap<>();
    public static final int PLAY_RATE10 = 2;
    protected long alCtx, alDevice;

    public void init() {
        AUDIO_EXECUTOR.submit(() -> {
            while (!Minecraft.getInstance().getSoundManager().soundEngine.loaded) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            }
            initAL();
            alDistanceModel(AL_EXPONENT_DISTANCE);
        });
        AUDIO_EXECUTOR.scheduleAtFixedRate(this::playing, 0, 10, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    protected void initAL() {
        var library = Minecraft.getInstance().getSoundManager().soundEngine.library;
        alCtx = library.context;
        alDevice = library.currentDevice;
        alcMakeContextCurrent(alCtx);
        AL.createCapabilities(ALC.createCapabilities(alDevice));
    }

    public void handle(BaseAudioPacket2C packet) {
        var opus = packet.opus;
        var sampleRate = packet.sampleRate;
        var from = packet.from;
        try {
            var decoded = OpusHelper.decode(opus, sampleRate);
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var fromEntity = level.getEntity(from);
            if (fromEntity instanceof Player player) {
                playerAudios.compute(player.getUUID(), (id, old) -> {
                    if (old == null) {
                        return new PlayerAudio(id, sampleRate);
                    } else if (old.sampleRate != sampleRate) {
                        old.close();
                        return new PlayerAudio(id, sampleRate);
                    } else {
                        return old;
                    }
                }).push(decoded);
            }
        } catch (Exception e) {
            LogUtils.getLogger().error(e.toString());
        }
    }

    protected void playing() {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                reset();
                return;
            }
            var library = Minecraft.getInstance().getSoundManager().soundEngine.library;
            if (library.context != alCtx || library.currentDevice != alDevice) {
                reset();
                initAL();
            }
            playerAudios.entrySet().removeIf(entry -> play(level, entry.getKey(), entry.getValue()));
        } catch (Throwable e) {
            LogUtils.getLogger().error(e.toString());
        }
    }

    public void reset() {
        playerAudios.values().forEach(PlayerAudio::close);
        playerAudios.clear();
    }

    /**
     * @return true, if want to remove player and their audio. Must close audio before return true.
     */
    protected abstract boolean play(Level level, UUID playerId, PlayerAudio audio);
}
