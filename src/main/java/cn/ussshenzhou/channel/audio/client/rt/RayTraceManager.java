package cn.ussshenzhou.channel.audio.client.rt;

import cn.ussshenzhou.channel.audio.client.receive.Audio;
import cn.ussshenzhou.channel.audio.client.receive.AudioManager;
import cn.ussshenzhou.channel.audio.client.receive.AudioReceiveHandler;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import com.mojang.logging.LogUtils;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceCalculator.*;
import static org.lwjgl.openal.EXTEfx.*;

/**
 * @author USS_Shenzhou
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(Dist.CLIENT)
public class RayTraceManager {
    static final int MAX_DISTANCE = 64;
    static final int MAX_BOUNCE_ROUND = 5;
    static final ArrayList<HitPoint> HIT_POINTS = new ArrayList<>();
    private static int AUX_SLOT = -1, REVERB_EFFECT;
    private static final ConcurrentHashMap<Vec3, SourceAudioData> SOURCE_AUDIO_DATA_CACHE = new ConcurrentHashMap<>();
    static boolean inWater = false;
    public static ClipContext.Block CHANNEL_OUTLINE = ClipContext.Block.valueOf("CHANNEL_OUTLINE");
    public static ClipContext.Block CHANNEL_VISUAL = ClipContext.Block.valueOf("CHANNEL_VISUAL");
    public static ForkJoinPool RAYTRACE_THREADS;

    static {
        var index = new AtomicInteger();
        RAYTRACE_THREADS = new ForkJoinPool(8, pool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Channel-RayTrace-Thread-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }, null, false);
    }

    public static int getSlot() {
        if (AUX_SLOT == -1) {
            AUX_SLOT = alGenAuxiliaryEffectSlots();
            REVERB_EFFECT = alGenEffects();
            alEffecti(REVERB_EFFECT, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        }
        return AUX_SLOT;
    }

    public static boolean play(Audio audio, Vec3 sourcePos) {
        if (sourcePos.distanceToSqr(AudioHelper.getEarPos()) > MAX_DISTANCE * MAX_DISTANCE) {
            audio.read(AudioReceiveHandler.PLAY_RATE10);
            return true;
        }
        var data = SOURCE_AUDIO_DATA_CACHE.compute(sourcePos, (p, _) -> RayTraceCalculator.calculateSourceAudioData(p));
        audio.updateSourceParameters(data, getSlot());
        return audio.play();
    }

    private static long lastUpdate = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null || !ChannelClientConfig.get().rayTraceAudio) {
            return;
        }
        var block = Minecraft.getInstance().level.getBlockState(BlockPos.containing(AudioHelper.getEarPos()));
        inWater = !block.getFluidState().isEmpty();
        AudioManager.AUDIO_EXECUTOR.execute(RayTraceManager::updateReflectionPan);
        if (Util.getMillis() - lastUpdate < 500) {
            return;
        }
        lastUpdate = Util.getMillis();
        clear();
        update();
    }

    private static void clear() {
        synchronized (HIT_POINTS) {
            HIT_POINTS.clear();
        }
        DebugRayTrace.rays.clear();
        SOURCE_AUDIO_DATA_CACHE.clear();
    }

    private static void update() {
        generateHitPoints();
        RayTraceCalculator.run();
        AudioManager.AUDIO_EXECUTOR.execute(RayTraceManager::setEaxReverb);
    }

    private static void generateHitPoints() {
        long time = Util.getNanos();

        var level = Minecraft.getInstance().level;
        boolean debug = ChannelClientConfig.get().showRaytrace;
        var earPos = AudioHelper.getEarPos();
        RAYTRACE_THREADS.submit(() -> generateRays(getRayAmount(), 0).parallelStream().forEach(ray -> {
            //noinspection DataFlowIssue
            generateOneRay(earPos, ray, level, debug, CHANNEL_OUTLINE, 0x80ffffff);
        })).join();
        RAYTRACE_THREADS.submit(() -> generateRays(getRayAmount(), 0).parallelStream().forEach(ray -> {
            //noinspection DataFlowIssue
            generateOneRay(earPos, ray, level, debug, CHANNEL_VISUAL, 0x8000ff00);
        })).join();

        LogUtils.getLogger().warn("{} ms", (Util.getNanos() - time) / 1000_000f);
    }

    private static void generateOneRay(Vec3 earPos, Vector3d ray, ClientLevel level, boolean debug, ClipContext.Block blockCollision, int color) {
        float journey = 0;
        var startPos = new Vec3(earPos.x, earPos.y, earPos.z);
        for (int round = 0; round < MAX_BOUNCE_ROUND; round++) {
            if (journey >= MAX_DISTANCE) {
                break;
            }
            var hitResult = shoot(startPos, startPos.add(ray.x * MAX_DISTANCE, ray.y * MAX_DISTANCE, ray.z * MAX_DISTANCE), blockCollision);
            if (hitResult.getType() == HitResult.Type.MISS) {
                break;
            }
            float distance = (float) hitResult.getLocation().distanceTo(startPos);
            if (distance == 0) {
                break;
            }
            journey += distance;
            var hitPos = hitResult.getLocation();
            var blockPos = hitResult.getBlockPos();
            var block = level.getBlockState(blockPos);
            var soundType = block.getSoundType(level, blockPos, null);
            synchronized (HIT_POINTS) {
                HIT_POINTS.add(new HitPoint(round, new Vec3(hitPos.x, hitPos.y, hitPos.z), journey, distance, BlockSoundProperty.get(soundType), hitResult.getDirection()));
            }
            if (debug) {
                DebugRayTrace.rays.add(new DebugRayTrace.Ray(
                        new Vec3(startPos.x, startPos.y, startPos.z),
                        new Vec3(startPos.x + ray.x * distance, startPos.y + ray.y * distance, startPos.z + ray.z * distance),
                        color
                ));
            }
            switch (hitResult.getDirection()) {
                case UP, DOWN -> ray.mul(1, -1, 1);
                case NORTH, SOUTH -> ray.mul(1, 1, -1);
                case EAST, WEST -> ray.mul(-1, 1, 1);
            }
            startPos = hitPos;
        }
    }

    private static void setEaxReverb() {
        getSlot();
        //-----density-----
        // = distribution of journey
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DENSITY, density);
        //-----diffusion-----
        // = surface roughness
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DIFFUSION, diffusion);
        //-----Gain-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_GAIN, 1);
        //-----HF/LF Gain-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_GAINHF, hfGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_GAINLF, 1);
        //-----RT60 RT60LF/HF-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_TIME, rt60);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_HFRATIO, inWater ? 0.25f : hfGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_LFRATIO, 1);
        //-----early reflection-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_GAIN, earlyRefGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_DELAY, earlyRefDelay);
        //-----late reflection-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_LATE_REVERB_GAIN, lateRefGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_LATE_REVERB_DELAY, lateRefDelay);
        //-----Echo-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_ECHO_TIME, Float.isNaN(echoTime) ? 0.25f : echoTime);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_ECHO_DEPTH, Float.isNaN(echoDepth) ? 0.05f : echoDepth);
        //-----Modulation-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_MODULATION_TIME, 0.4f);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_MODULATION_DEPTH, 0.025f);

        alAuxiliaryEffectSloti(AUX_SLOT, AL_EFFECTSLOT_EFFECT, REVERB_EFFECT);
    }

    private static void updateReflectionPan() {
        var earPos = AudioHelper.getEarPos();
        var inverseRotation = new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).conjugate();

        var earlyPos = new Vector3f(
                (float) (earlyRefX - earPos.x),
                (float) (earlyRefY - earPos.y),
                (float) (earlyRefZ - earPos.z)
        );
        earlyPos.rotate(inverseRotation);
        alEffectfv(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_PAN, new float[]{earlyPos.x, earlyPos.y, earlyPos.z});

        var latePos = new Vector3f(
                (float) (lateRefX - earPos.x),
                (float) (lateRefY - earPos.y),
                (float) (lateRefZ - earPos.z)
        );
        latePos.rotate(inverseRotation);
        alEffectfv(REVERB_EFFECT, AL_EAXREVERB_LATE_REVERB_PAN, new float[]{latePos.x, latePos.y, latePos.z});
    }

    protected static int getRayAmount() {
        return 300;
        //TODO multi call?
        //int averageHistory = ArrayHelper.absAverage(HIT_POINT_HISTORY);
        //if (averageHistory == 0) {
        //    return 200;
        //}
        //float delta = Mth.clamp((averageHistory - HIT_POINT_EXPECTATION) / (float) HIT_POINT_EXPECTATION, -1, 1);
        //int ray = (int) (HIT_POINT_EXPECTATION * (1 - Math.signum(delta) * Math.sqrt(Math.abs(Math.sin(delta)))));
        //return Mth.clamp(ray, HIT_POINT_EXPECTATION / MAX_BOUNCE_ROUND, HIT_POINT_EXPECTATION * MAX_BOUNCE_ROUND / 2);
    }

    private static ArrayList<Vector3d> generateRays(int N, double offset) {
        final double phi = Math.PI * (3 - Math.sqrt(5));
        var rays = new ArrayList<Vector3d>(N);
        for (int i = 0; i < N; i++) {
            double y = 1 - ((i + offset) / (float) (N - 1 + offset)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = phi * (i + offset);
            rays.add(new Vector3d(Math.cos(theta) * r, y, Math.sin(theta) * r));
        }
        return rays;
    }

    public static BlockHitResult shoot(Vec3 from, Vec3 to, ClipContext.Block blockCollision) {
        var clipContext = new ClipContext(
                from,
                to,
                blockCollision,
                inWater ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY,
                CollisionContext.empty()
        );
        return Minecraft.getInstance().level.clip(clipContext);
    }

    public static boolean canSee(Vec3 from, Vec3 to) {
        return shoot(from, to, CHANNEL_OUTLINE).getType() == HitResult.Type.MISS;
    }
}
