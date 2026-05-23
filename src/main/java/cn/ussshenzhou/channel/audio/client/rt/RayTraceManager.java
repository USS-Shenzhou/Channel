package cn.ussshenzhou.channel.audio.client.rt;

import cn.ussshenzhou.channel.audio.client.receive.BaseAudioManager;
import cn.ussshenzhou.channel.audio.client.receive.PlayerAudio;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
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

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceCalculator.*;
import static org.lwjgl.openal.AL11.AL_POSITION;
import static org.lwjgl.openal.AL11.alSource3f;
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

    public static int getSlot() {
        if (AUX_SLOT == -1) {
            AUX_SLOT = alGenAuxiliaryEffectSlots();
            REVERB_EFFECT = alGenEffects();
            alEffecti(REVERB_EFFECT, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        }
        return AUX_SLOT;
    }

    public static void play(double x, double y, double z, PlayerAudio audio) {
        var sourcePos = new Vec3(x, y, z);
        if (sourcePos.distanceToSqr(getEarPos()) > MAX_DISTANCE * MAX_DISTANCE) {
            audio.read(BaseAudioManager.PLAY_RATE10);
            return;
        }
        var data = SOURCE_AUDIO_DATA_CACHE.computeIfAbsent(sourcePos, RayTraceCalculator::calculateSourceAudioData);
        alFilterf(audio.alDirectFilter, AL_LOWPASS_GAIN, data.directGain());
        alFilterf(audio.alDirectFilter, AL_LOWPASS_GAINHF, data.directHF());
        alFilterf(audio.alReverbFilter, AL_LOWPASS_GAIN, data.reverbGain());
        alFilterf(audio.alReverbFilter, AL_LOWPASS_GAINHF, 1);
        alSource3f(audio.alSource, AL_POSITION, (float) data.virtualPos().x, (float) data.virtualPos().y, (float) data.virtualPos().z);
        audio.play();
    }

    static Vec3 getEarPos() {
        var mc = Minecraft.getInstance();
        if (ChannelClientConfig.get().showRaytrace && mc.getCameraEntity() != null) {
            return mc.getCameraEntity().getEyePosition();
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
    }

    private static long lastUpdate = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null || !ChannelClientConfig.get().rayTraceAudio) {
            return;
        }
        var block = Minecraft.getInstance().level.getBlockState(BlockPos.containing(getEarPos()));
        inWater = !block.getFluidState().isEmpty();
        BaseAudioManager.AUDIO_EXECUTOR.execute(RayTraceManager::updateReflectionPan);
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
        BaseAudioManager.AUDIO_EXECUTOR.execute(RayTraceManager::setEaxReverb);
    }

    private static void generateHitPoints() {
        synchronized (HIT_POINTS) {
            var level = Minecraft.getInstance().level;
            boolean debug = ChannelClientConfig.get().showRaytrace;
            var earPos = getEarPos();
            for (var ray : generateRays(getRayAmount(), 0)) {
                //noinspection DataFlowIssue
                generateOneRay(earPos, ray, level, debug, ClipContext.Block.OUTLINE, 0x80ffffff);
            }
            for (var ray : generateRays(getRayAmount(), 0.5)) {
                //noinspection DataFlowIssue
                generateOneRay(earPos, ray, level, debug, ClipContext.Block.VISUAL, 0x8000ff00);
            }
        }
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
            HIT_POINTS.add(new HitPoint(round, new Vec3(hitPos.x, hitPos.y, hitPos.z), journey, distance, BlockSoundProperty.get(soundType)));
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
        //TODO individual calculation
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_HFRATIO, inWater ? 0.25f : hfGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_LFRATIO, 1);
        //-----early reflection-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_GAIN, earlyRefGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_DELAY, earlyRefDelay);
        //-----late reflection-----
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_LATE_REVERB_GAIN, lateRefGain);
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_LATE_REVERB_DELAY, lateRefDelay);




            /* TODO
            // ==========================================
            // 5. 回声与调制 (Echo & Modulation)
            // ==========================================

            // [回声时间] 0.075 ~ 0.25 (默认: 0.25秒)
            // 循环回声的时间间隔
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_ECHO_TIME, 0.25f);

            // [回声深度] 0.0 ~ 1.0 (默认: 0.0)
            // 引入回声的量
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_ECHO_DEPTH, 0.0f);

            // [调制时间] 0.04 ~ 4.0 (默认: 0.25秒)
            // 音高调制的频率
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_MODULATION_TIME, 0.25f);

            // [调制深度] 0.0 ~ 1.0 (默认: 0.0)
            // 音高变化的幅度（颤音效果）
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_MODULATION_DEPTH, 0.0f);


            // ==========================================
            // 6. 物理微调 (Advanced Physical)
            // ==========================================

            // [空气吸收增益] 0.892 ~ 1.0 (默认: 0.994)
            // 空气对高频的阻尼
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 0.994f);

            // [高频参考值] 1000.0 ~ 20000.0 (默认: 5000.0)
            // 定义高频的频率阈值
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_HFREFERENCE, 5000.0f);

            // [低频参考值] 20.0 ~ 1000.0 (默认: 250.0)
            // 定义低频的频率阈值
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_LFREFERENCE, 250.0f);

            // [房间滚降因子] 0.0 ~ 10.0 (默认: 0.0)
            // 混响随距离衰减的效果（0为关闭）
            alEffectf(REVERB_EFFECT, AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, 0.0f);

            // [高频衰减限制] 0 或 1 (默认: 1/True)
            // *注：整数/布尔参数需使用 alEffecti
            alEffecti(REVERB_EFFECT, AL_EAXREVERB_DECAY_HFLIMIT, 1);*/

        alAuxiliaryEffectSloti(AUX_SLOT, AL_EFFECTSLOT_EFFECT, REVERB_EFFECT);
    }

    private static void updateReflectionPan() {
        var earPos = getEarPos();
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
        return shoot(from, to, ClipContext.Block.OUTLINE).getType() == HitResult.Type.MISS;
    }
}
