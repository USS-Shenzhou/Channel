package cn.ussshenzhou.channel.audio.client.rt;

import net.minecraft.util.Mth;

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
            density = Mth.clamp(journeyStandardDeviation / (MAX_DISTANCE / 2f), 0, 1);
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
            hfGain = Mth.clamp((float) Math.sqrt(airHfGain * hfWeightedTotal / weightTotal), 0, 1);
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
            openSpaceCorrection = Mth.clamp(pointTotalAmount / ((float) getRayAmount() * MAX_BOUNCE_ROUND), 0, 1);
            double r = collisionCount * distanceMean / 340 * openSpaceCorrection;
            rt60 = (float) Mth.clamp(20 - 400 / (r + 20), 0.1, 20);
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
            earlyRefGain = Mth.clamp(2 * (float) Math.sqrt(earlyRefGainWeightedTotal / earlyRefPointAmount) * openSpaceCorrection, 0, 3.16f);
            earlyRefJourneyWeightedMean = (float) earlyRefJourneyWeightedTotal / earlyRefWeightedTotal;
            earlyRefDelay = (float) (earlyRefJourneyWeightedMean / 340);

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
            lateRefDelay = (float) (lateRefJourneyWeightedMean / 340) - earlyRefDelay;

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

}
