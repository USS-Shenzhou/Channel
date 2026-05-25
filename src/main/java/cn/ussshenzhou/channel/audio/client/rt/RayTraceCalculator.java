package cn.ussshenzhou.channel.audio.client.rt;

import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.function.Consumer;

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceManager.*;

/**
 * @author USS_Shenzhou
 */
public class RayTraceCalculator {
    private static final ArrayList<Runnable> DEFAULT = new ArrayList<>();
    private static final ArrayList<Runnable> PRE = new ArrayList<>();
    private static final ArrayList<Consumer<HitPoint>> LOOP1 = new ArrayList<>();
    private static final ArrayList<Runnable> MID = new ArrayList<>();
    private static final ArrayList<Consumer<HitPoint>> LOOP2 = new ArrayList<>();
    private static final ArrayList<Runnable> POST = new ArrayList<>();

    static volatile int pointTotalAmount = 0;

    public static void run() {
        synchronized (HIT_POINTS) {
            pointTotalAmount = HIT_POINTS.size();
            if (pointTotalAmount < 2) {
                DEFAULT.forEach(Runnable::run);
                return;
            }
            PRE.forEach(Runnable::run);
            for (HitPoint hitPoint : HIT_POINTS) {
                LOOP1.forEach(c -> c.accept(hitPoint));
            }
            MID.forEach(Runnable::run);
            for (HitPoint hitPoint : HIT_POINTS) {
                LOOP2.forEach(c -> c.accept(hitPoint));
            }
            POST.forEach(Runnable::run);
        }
    }

    //----------Density----------
    static volatile float density;
    static double journeySum;
    static float journeyMean;
    static float journeySquaredDiffSum;
    static float journeyStandardDeviation;

    static {
        def(() -> density = 0);
        pre(() -> journeySum = 0);
        loop1(h -> journeySum += h.journey());
        mid(() -> {
            journeyMean = (float) (journeySum / pointTotalAmount);
            journeySquaredDiffSum = 0;
        });
        loop2(h -> {
            float diff = h.journey() - journeyMean;
            journeySquaredDiffSum += diff * diff;
        });
        post(() -> {
            journeyStandardDeviation = (float) Math.sqrt(journeySquaredDiffSum / pointTotalAmount);
            density = Mth.clamp(journeyStandardDeviation / (MAX_DISTANCE / 2f) + (inWater ? 0.6f : 0), 0, 1);
        });
    }

    //----------Diffusion----------
    static volatile float diffusion;
    static double roughnessWeightedTotal;
    static double weightTotal;
    //decay time/RT60
    static double absorptionWeightedTotal;

    static {
        def(() -> {
            diffusion = 0;
        });
        pre(() -> {
            weightTotal = 0;
            roughnessWeightedTotal = 0;
            absorptionWeightedTotal = 0;
        });
        loop1(h -> {
            double weight = h.weight();
            roughnessWeightedTotal += h.soundProperty().roughness() * weight;
            absorptionWeightedTotal += h.soundProperty().absorption() * weight;
            weightTotal += weight;
        });
        post(() -> {
            diffusion = Mth.clamp((float) (roughnessWeightedTotal / weightTotal), 0, 1);
        });
    }

    //----------HF Gain----------
    static volatile float hfGain;

    static double hfWeightedTotal;

    static {
        def(() -> {
            hfGain = 0.9f;
        });
        pre(() -> {
            hfWeightedTotal = 0;
        });
        loop1(h -> {
            double weight = h.weight();
            hfWeightedTotal += h.soundProperty().hfGain() * weight;
        });
        post(() -> {
            double airHfGain = Math.pow(0.99, journeyMean);
            hfGain = Mth.clamp((float) Math.sqrt(airHfGain * hfWeightedTotal / weightTotal) * (inWater ? 0.15f : 1), 0, 2);
        });
    }

    //----------decay time/RT60----------
    static volatile float rt60;
    static double distanceSum;
    static float distanceMean;
    //absorptionWeightedTotal;
    static float openSpaceCorrection;


    static {
        def(() -> {
            rt60 = 1.5f;
        });
        pre(() -> {
            distanceSum = 0;
            distanceMean = 0;
        });
        loop1(h -> {
            distanceSum += h.distance();
        });
        mid(() -> {
            distanceMean = (float) (distanceSum / pointTotalAmount);
        });
        post(() -> {
            double collisionCount = -6 / Math.log10(1 - absorptionWeightedTotal / weightTotal);
            openSpaceCorrection = inWater ? 1 : Mth.clamp(pointTotalAmount / ((float) getRayAmount() * MAX_BOUNCE_ROUND * 2), 0, 1);
            double r = collisionCount * distanceMean / 340 * openSpaceCorrection;
            rt60 = (float) Mth.clamp((20 - 400 / (r + 20)) * (inWater ? 3.6f : 1), 0.1, 20);
        });
    }

    //----------Early Ref----------
    static volatile float earlyRefGain;
    static double earlyRefGainWeightedTotal;
    static int earlyRefPointAmount;

    static volatile float earlyRefDelay;
    static double earlyRefWeightedTotal;
    static double earlyRefJourneyWeightedTotal;
    static double earlyRefJourneyWeightedMean;

    static volatile double earlyRefX, earlyRefY, earlyRefZ;
    static double earlyRefXWeightedTotal, earlyRefYWeightedTotal, earlyRefZWeightedTotal;

    static {
        def(() -> {
            earlyRefGain = 0.05f;
            earlyRefDelay = 0.007f;
            earlyRefX = earlyRefY = earlyRefZ = 0;
        });
        pre(() -> {
            earlyRefGainWeightedTotal = 0;
            earlyRefPointAmount = 0;
            earlyRefWeightedTotal = 0;
            earlyRefJourneyWeightedTotal = 0;
            earlyRefJourneyWeightedMean = 0;
            earlyRefXWeightedTotal = 0;
            earlyRefYWeightedTotal = 0;
            earlyRefZWeightedTotal = 0;
        });
        loop1(h -> {
            if (h.journey() <= 17) {
                double weight = h.weight();
                earlyRefGainWeightedTotal += (1 - h.soundProperty().absorption()) * weight;
                earlyRefPointAmount++;
                earlyRefJourneyWeightedTotal += h.journey() * weight;
                earlyRefWeightedTotal += weight;
                earlyRefXWeightedTotal += h.pos().x * weight;
                earlyRefYWeightedTotal += h.pos().y * weight;
                earlyRefZWeightedTotal += h.pos().z * weight;
            }
        });
        post(() -> {
            earlyRefGain = Mth.clamp(2 * (float) Math.sqrt(earlyRefGainWeightedTotal / earlyRefPointAmount) * openSpaceCorrection * (inWater ? 1.2f : 1), 0, 3.16f);
            earlyRefJourneyWeightedMean = (float) earlyRefJourneyWeightedTotal / earlyRefWeightedTotal;
            earlyRefDelay = (float) (earlyRefJourneyWeightedMean / getSoundSpeed());

            earlyRefX = earlyRefXWeightedTotal / earlyRefWeightedTotal;
            earlyRefY = earlyRefYWeightedTotal / earlyRefWeightedTotal;
            earlyRefZ = earlyRefZWeightedTotal / earlyRefWeightedTotal;
        });
    }


    //----------Late Ref----------
    static volatile float lateRefGain;
    static double lateRefGainWeightedTotal;
    static int lateRefPointAmount;

    static volatile float lateRefDelay;
    static double lateRefWeightedTotal;
    static double lateRefJourneyWeightedTotal;
    static double lateRefJourneyWeightedMean;

    static volatile double lateRefX, lateRefY, lateRefZ;
    static double lateRefXWeightedTotal, lateRefYWeightedTotal, lateRefZWeightedTotal;

    static double reflectionWeightTotal = 0;

    static {
        def(() -> {
            lateRefGain = 0.5f;
            lateRefDelay = 0.01f;
            lateRefX = lateRefY = lateRefZ = 0;
        });
        pre(() -> {
            lateRefGainWeightedTotal = 0;
            lateRefPointAmount = 0;
            lateRefWeightedTotal = 0;
            lateRefJourneyWeightedTotal = 0;
            lateRefJourneyWeightedMean = 0;
            lateRefXWeightedTotal = 0;
            lateRefYWeightedTotal = 0;
            lateRefZWeightedTotal = 0;
        });
        mid(() -> {
            reflectionWeightTotal = weightTotal * (1 - absorptionWeightedTotal) / absorptionWeightedTotal;
        });
        loop1(h -> {
            if (h.journey() > 17 && h.journey() <= 34) {
                double weight = h.weight();
                lateRefGainWeightedTotal += (1 - h.soundProperty().absorption()) * weight;
                lateRefPointAmount++;
                lateRefJourneyWeightedTotal += h.journey() * weight;
                lateRefWeightedTotal += weight;
                lateRefXWeightedTotal += h.pos().x * weight;
                lateRefYWeightedTotal += h.pos().y * weight;
                lateRefZWeightedTotal += h.pos().z * weight;

            }
        });
        post(() -> {
            lateRefGain = Mth.clamp(20 * (float) Math.sqrt(lateRefGainWeightedTotal / lateRefPointAmount) * openSpaceCorrection, 0, 10);
            lateRefJourneyWeightedMean = (float) lateRefJourneyWeightedTotal / lateRefWeightedTotal;
            lateRefDelay = (float) (lateRefJourneyWeightedMean / getSoundSpeed()) - earlyRefDelay;

            lateRefX = lateRefXWeightedTotal / lateRefWeightedTotal;
            lateRefY = lateRefYWeightedTotal / lateRefWeightedTotal;
            lateRefZ = lateRefZWeightedTotal / lateRefWeightedTotal;
        });
    }

    //----------A----------
    static {
        def(() -> {

        });
        pre(() -> {

        });
        loop1(h -> {

        });
        mid(() -> {

        });
        loop2(h -> {

        });
        post(() -> {

        });
    }


    //----------Util----------
    private static void def(Runnable r) {
        DEFAULT.add(r);
    }

    private static void pre(Runnable r) {
        PRE.add(r);
    }

    private static void loop1(Consumer<HitPoint> c) {
        LOOP1.add(c);
    }

    private static void mid(Runnable r) {
        MID.add(r);
    }

    private static void loop2(Consumer<HitPoint> c) {
        LOOP2.add(c);
    }

    private static void post(Runnable r) {
        POST.add(r);
    }

    private static int getSoundSpeed() {
        return inWater ? 1500 : 340;
    }

    //----------Direct Sound----------
    static SourceAudioData calculateSourceAudioData(Vec3 sourcePos) {
        var cameraPos = getEarPos();
        var tuple = calculateVirtualDirection(sourcePos);
        double meanReverbWeight = tuple.getA();
        var hitA = shoot(sourcePos, cameraPos, ClipContext.Block.OUTLINE);
        float wallThickness;
        if (hitA.getType() == HitResult.Type.MISS) {
            wallThickness = 0;
        } else {
            var hitB = shoot(cameraPos, sourcePos, ClipContext.Block.OUTLINE);
            wallThickness = (float) hitA.getLocation().distanceTo(hitB.getLocation());
        }
        double wallDecay = Math.pow(inWater ? 0.5 : 0.25, wallThickness);
        double directWeight = wallDecay / Math.max(1, cameraPos.distanceTo(sourcePos));
        var virtualPos = tuple.getB();
        double directDirectionWeight = 10 * Math.pow(inWater ? 0.25 : 0.1, wallThickness);
        var finalPos = virtualPos.scale(meanReverbWeight).add(sourcePos.scale(directDirectionWeight)).scale(1 / (meanReverbWeight + directDirectionWeight));
        float directGain = (float) (Math.min(directWeight, 1));
        float directHF = (float) wallDecay;
        float reverbGain = (float) (Math.sqrt(Math.min(1 - Math.pow(0.001, meanReverbWeight), 1)));
        if (inWater) {
            directHF *= 0.15f;
            reverbGain = Mth.clamp(reverbGain * 1.3f, 0, 1);
        }
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

    //----------Debug----------

    public static float getDiffusion() {
        return diffusion;
    }

    public static float getDensity() {
        return density;
    }

    public static float getHfGain() {
        return hfGain;
    }

    public static float getRt60() {
        return rt60;
    }

    public static float getEarlyRefGain() {
        return earlyRefGain;
    }

    public static float getEarlyRefDelay() {
        return earlyRefDelay;
    }

    public static float getLateRefGain() {
        return lateRefGain;
    }

    public static float getLateRefDelay() {
        return lateRefDelay;
    }

    public static float getOpenSpaceCorrection() {
        return openSpaceCorrection;
    }
}
