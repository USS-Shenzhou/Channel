package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.client.rt.RayTraceManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_EXPONENT_DISTANCE;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;

@EventBusSubscriber(Dist.CLIENT)
public class AudioManager {
    public static final ScheduledExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r ->
            Thread.ofPlatform()
                    .name("Channel-Audio-Play-Thread")
                    .daemon(true)
                    .factory()
                    .newThread(r)
    );
    static final ConcurrentHashMap<Integer, Audio> audios = new ConcurrentHashMap<>();
    static long alCtx;
    static long alDevice;

    @SubscribeEvent
    public static void onExit(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    public static void init() {
        AUDIO_EXECUTOR.execute(() -> {
            while (!Minecraft.getInstance().getSoundManager().soundEngine.loaded) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            }
            initAL();
            alDistanceModel(AL_EXPONENT_DISTANCE);
        });
        AUDIO_EXECUTOR.scheduleAtFixedRate(AudioManager::playing, 0, 10, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    protected static void initAL() {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(15));
        var library = Minecraft.getInstance().getSoundManager().soundEngine.library;
        alCtx = library.context;
        alDevice = library.currentDevice;
        alcMakeContextCurrent(alCtx);
        AL.createCapabilities(ALC.createCapabilities(alDevice));
    }

    protected static void playing() {
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
            audios.entrySet().removeIf(e -> play(level, e.getValue()));
        } catch (Throwable e) {
            LogUtils.getLogger().error("Something went wrong, but it should be okay. You can ignore this if nothing else went wrong.");
            LogUtils.getLogger().error(e.toString(), e);
        }
    }

    public static void reset() {
        audios.values().forEach(Audio::close);
        audios.clear();
    }

    /**
     * @return true, if want to remove player and their audio. Must close audio before return true.
     */
    protected static boolean play(Level level, Audio audio) {
        var pos = audio.getPos(level);
        if (ChannelClientConfig.get().rayTraceAudio) {
            return RayTraceManager.play(audio, pos);
        } else {
            return simplePlay(audio, pos);
        }
    }

    private static boolean simplePlay(Audio audio, Vec3 pos) {
        alSource3f(audio.alSource, AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        return audio.play();
    }
}
