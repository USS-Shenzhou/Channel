package cn.ussshenzhou.channel.util;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.Util;

/**
 * @author USS_Shenzhou
 */
public class TimeCounter {
    private final Long2IntOpenHashMap container = new Long2IntOpenHashMap();
    private final int windowSizeMs;

    public TimeCounter(int windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
    }

    public synchronized void update() {
        long now = Util.getMillis();
        container.keySet().removeIf(then -> now - then > windowSizeMs);
    }

    public synchronized void put(int value) {
        update();
        container.put(Util.getMillis(), value);
    }

    public synchronized void add(int value) {
        update();
        container.addTo(Util.getMillis(), value);
    }

    public synchronized double averageIn1s() {
        update();
        return container.values().intStream().sum() / (double) windowSizeMs * 1000;
    }

    public synchronized int count() {
        update();
        return container.values().intStream().sum();
    }

    public synchronized String getStringAsMs() {
        update();
        if (container.isEmpty()) {
            return "avg §b0.00ms§r    max §a0.00ms§r    stb §a100.0%§r";
        }
        int sum = 0;
        int max = 0;
        for (int v : container.values()) {
            sum += v;
            if (v > max) {
                max = v;
            }
        }
        int average = sum / container.size();
        double stability = 0;
        if (average > 0) {
            long deviationSum = 0;
            for (int v : container.values()) {
                deviationSum += Math.abs(v - average);
            }
            stability = 1 - (double) deviationSum / container.size() / average;
        }
        double avgMs = average / 1000d;
        double maxMs = max / 1000d;
        double maxDev = average == 0 ? 0 : Math.abs((double) (max - average) / average * 100);
        double stbDevPercent = (1 - stability) * 100d;
        return "avg §b%.2fms§r    max %s%.2fms§r    stb %s%.1f%%§r".formatted(
                avgMs,
                colorByDeviation(maxDev),
                maxMs,
                colorByDeviation(stbDevPercent),
                stability * 100
        );
    }

    private static String colorByDeviation(double absDeviationPercent) {
        if (absDeviationPercent <= 5) {
            return "§a";
        } else if (absDeviationPercent <= 10) {
            return "§e";
        } else if (absDeviationPercent <= 20) {
            return "§6";
        } else {
            return "§c";
        }
    }
}
