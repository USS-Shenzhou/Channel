package cn.ussshenzhou.channel.audio.client.rt;

import cn.ussshenzhou.channel.audio.client.receive.BaseAudioManager;
import cn.ussshenzhou.channel.audio.client.receive.PlayerAudio;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import com.google.common.collect.EvictingQueue;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceCalculator.*;
import static org.lwjgl.openal.EXTEfx.*;
import static org.lwjgl.openal.AL11.*;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class RayTraceManager {
    private static final EvictingQueue<Integer> HIT_POINT_HISTORY = EvictingQueue.create(10);
    static final int MAX_DISTANCE = 64;
    static final int MAX_BOUNCE_ROUND = 5;
    static final ArrayList<HitPoint> HIT_POINTS = new ArrayList<>();
    private static final float INFINITE_DISTANCE = -1;
    private static int AUX_SLOT = -1, REVERB_EFFECT;
    private static final ConcurrentHashMap<Vec3, SourceAudioData> SOURCE_AUDIO_DATA_CACHE = new ConcurrentHashMap<>();

    public static int getSlot() {
        if (AUX_SLOT == -1) {
            AUX_SLOT = alGenAuxiliaryEffectSlots();
            REVERB_EFFECT = alGenEffects();
            alEffecti(REVERB_EFFECT, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        }
        return AUX_SLOT;
    }

    public static void play(double x, double y, double z, PlayerAudio audio) {
        long t = Util.getNanos();
        var sourcePos = new Vec3(x, y, z);
        if (sourcePos.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > MAX_DISTANCE * MAX_DISTANCE) {
            audio.read(BaseAudioManager.PLAY_RATE10);
            return;
        }
        var data = SOURCE_AUDIO_DATA_CACHE.computeIfAbsent(sourcePos, RayTraceManager::calculateSourceAudioData);
        alFilterf(audio.alDirectFilter, AL_LOWPASS_GAIN, data.directGain());
        alFilterf(audio.alDirectFilter, AL_LOWPASS_GAINHF, data.directHF());
        alFilterf(audio.alReverbFilter, AL_LOWPASS_GAIN, data.reverbGain());
        alFilterf(audio.alReverbFilter, AL_LOWPASS_GAINHF, 1);
        alSource3f(audio.alSource, AL_POSITION, (float) data.virtualPos().x, (float) data.virtualPos().y, (float) data.virtualPos().z);
        audio.play();
    }

    private static SourceAudioData calculateSourceAudioData(Vec3 sourcePos) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var tuple = calculateVirtualDirection(sourcePos);
        double meanReverbWeight = tuple.getA();
        var hitA = shoot(sourcePos, cameraPos);
        float wallThickness;
        if (hitA.getType() == HitResult.Type.MISS) {
            wallThickness = 0;
        } else {
            var hitB = shoot(cameraPos, sourcePos);
            wallThickness = (float) hitA.getLocation().distanceTo(hitB.getLocation());
        }
        double wallDecay = Math.pow(0.25, wallThickness);
        double directWeight = wallDecay / Math.max(0.2, cameraPos.distanceTo(sourcePos));
        var virtualPos = tuple.getB();
        double directDirectionWeight = 10 * Math.pow(0.1, wallThickness);
        var finalPos = virtualPos.scale(meanReverbWeight).add(sourcePos.scale(directDirectionWeight)).scale(1 / (meanReverbWeight + directDirectionWeight));
        float directGain = (float) (Math.sqrt(Math.min(directWeight, 1)));
        float directHF = (float) wallDecay;
        float reverbGain = (float) (Math.sqrt(Math.min(1 - Math.pow(0.001, meanReverbWeight), 1)));
        return new SourceAudioData(directGain, directHF, reverbGain, finalPos);
    }

    private static Tuple<Double, Vec3> calculateVirtualDirection(Vec3 sourcePos) {
        synchronized (HIT_POINTS) {
            double weightXSum = 0, weightYSum = 0, weightZSum = 0, totalWeight = 0;
            int n = 0;
            flag:
            for (int i = 0; i < HIT_POINTS.size(); ++i) {
                var hitPoint = HIT_POINTS.get(i);
                if (hitPoint.round() != 0) {
                    continue;
                }
                HitPoint subHitPoint = null;
                double journeyDecay = 1;

                if (canSee(sourcePos, hitPoint.pos())) {
                    subHitPoint = hitPoint;
                    journeyDecay = 1 - hitPoint.soundProperty().absorption();
                } else if (i == HIT_POINTS.size() - 1) {
                    break;
                } else {
                    for (int j = 1; j < MAX_BOUNCE_ROUND; j++) {
                        var p = HIT_POINTS.get(i + j);
                        if (p.round() == 0) {
                            continue flag;
                        }
                        journeyDecay *= (1 - p.soundProperty().absorption());
                        if (canSee(sourcePos, p.pos())) {
                            subHitPoint = p;
                            break;
                        }
                    }
                    if (subHitPoint == null) {
                        continue;
                    }
                }
                double distance = sourcePos.distanceTo(subHitPoint.pos());
                double journey = distance + subHitPoint.journey();
                double weight = journeyDecay / (journey * journey);
                weightXSum += weight * hitPoint.pos().x;
                weightYSum += weight * hitPoint.pos().y;
                weightZSum += weight * hitPoint.pos().z;
                totalWeight += weight;
                n++;
            }
            if (totalWeight == 0) {
                return new Tuple<>(0d, new Vec3(0, 0, 0));
            }
            return new Tuple<>(totalWeight / n, new Vec3(weightXSum / totalWeight, weightYSum / totalWeight, weightZSum / totalWeight));
        }
    }

    private static long lastUpdate = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null || !ChannelClientConfig.get().rayTraceAudio) {
            return;
        }
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
            HIT_POINT_HISTORY.add(HIT_POINTS.size());
            HIT_POINTS.clear();
        }
        DebugRayTrace.whiteRays.clear();
        SOURCE_AUDIO_DATA_CACHE.clear();
    }

    private static void update() {
        var player = Minecraft.getInstance().getCameraEntity();
        if (player == null) {
            return;
        }
        var earPos = player.getEyePosition();
        generateHitPoints(earPos);
        RayTraceCalculator.run();
        BaseAudioManager.AUDIO_EXECUTOR.execute(RayTraceManager::setEaxReverb);
    }

    private static void generateHitPoints(Vec3 earPos) {
        synchronized (HIT_POINTS) {
            var level = Minecraft.getInstance().level;
            for (var ray : generateWhiteRay(getRayAmount())) {
                float journey = 0;
                var startPos = new Vec3(earPos.x, earPos.y, earPos.z);
                for (int round = 0; round < MAX_BOUNCE_ROUND; round++) {
                    if (journey >= MAX_DISTANCE) {
                        break;
                    }
                    //TODO in water
                    var hitResult = shoot(startPos, startPos.add(ray.x * MAX_DISTANCE, ray.y * MAX_DISTANCE, ray.z * MAX_DISTANCE));
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

                    DebugRayTrace.whiteRays.put(new Vector3f((float) startPos.x, (float) startPos.y, (float) startPos.z), new Vec3(ray.x * distance, ray.y * distance, ray.z * distance));

                    switch (hitResult.getDirection()) {
                        case UP, DOWN -> ray.mul(1, -1, 1);
                        case NORTH, SOUTH -> ray.mul(1, 1, -1);
                        case EAST, WEST -> ray.mul(-1, 1, 1);
                    }
                    startPos = hitPos;
                }
            }
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
        alEffectf(REVERB_EFFECT, AL_EAXREVERB_DECAY_HFRATIO, hfGain);
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
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var camPos = camera.getPosition();
        var inverseRotation = new Quaternionf(camera.rotation()).conjugate();

        var earlyPos = new Vector3f(
                (float) (earlyRefX - camPos.x),
                (float) (earlyRefY - camPos.y),
                (float) (earlyRefZ - camPos.z)
        );
        earlyPos.rotate(inverseRotation);
        alEffectfv(REVERB_EFFECT, AL_EAXREVERB_REFLECTIONS_PAN, new float[]{earlyPos.x, earlyPos.y, earlyPos.z});

        var latePos = new Vector3f(
                (float) (lateRefX - camPos.x),
                (float) (lateRefY - camPos.y),
                (float) (lateRefZ - camPos.z)
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

    private static ArrayList<Vector3d> generateWhiteRay(int N) {
        final double phi = Math.PI * (3 - Math.sqrt(5));
        var rays = new ArrayList<Vector3d>(N);
        for (int i = 0; i < N; i++) {
            double y = 1 - (i / (float) (N - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = phi * i;
            rays.add(new Vector3d(
                    Math.cos(theta) * r,
                    y,
                    Math.sin(theta) * r
            ));
        }
        return rays;
    }

    public static BlockHitResult shoot(Vec3 from, Vec3 to) {
        var clipContext = new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                CollisionContext.empty()
        );
        return Minecraft.getInstance().level.clip(clipContext);
    }

    public static boolean canSee(Vec3 from, Vec3 to) {
        return shoot(from, to).getType() == HitResult.Type.MISS;
    }

    public static boolean canSee(double x0, double y0, double z0, double x1, double y1, double z1) {
        return canSee(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1));
    }

    public static boolean canSee(Vec3 from, double x1, double y1, double z1) {
        return canSee(from, new Vec3(x1, y1, z1));
    }
}
