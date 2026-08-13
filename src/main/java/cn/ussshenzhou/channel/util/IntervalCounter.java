package cn.ussshenzhou.channel.util;

import net.minecraft.util.Util;

public class IntervalCounter {
    private long idealIntervalUs;
    private final long windowUs;
    private long[] timestamps;
    private int head = 0;
    private int size = 0;
    private final boolean unknownIdeal;

    private long cachedAverageIntervalUs;
    private long cachedMaxIntervalUs;
    private double cachedStability;

    public IntervalCounter(double idealIntervalMs, double windowMs) {
        this.idealIntervalUs = (long) (idealIntervalMs * 1000);
        this.windowUs = (long) (windowMs * 1000);
        this.timestamps = new long[(int) (windowUs / idealIntervalUs) * 2];
        this.cachedAverageIntervalUs = idealIntervalUs;
        this.cachedMaxIntervalUs = idealIntervalUs;
        this.unknownIdeal = false;
    }

    public IntervalCounter(double windowMs) {
        this.idealIntervalUs = 0;
        this.windowUs = (long) (windowMs * 1000);
        this.timestamps = new long[(int) (windowUs / 10_000) * 2];
        this.cachedAverageIntervalUs = 0;
        this.cachedMaxIntervalUs = 0;
        this.unknownIdeal = true;
    }

    public synchronized void update() {
        long now = Util.getNanos() / 1000;
        evict(now);
        timestamps[head] = now;
        head = (head + 1) % timestamps.length;
        if (size < timestamps.length) {
            size++;
        }
        updateStatistics();
    }

    private void evict(long now) {
        long cutoff = now - windowUs;
        while (size > 0) {
            int tail = tail();
            if (timestamps[tail] < cutoff) {
                size--;
            } else {
                break;
            }
        }
    }

    private int tail() {
        return (head - size + timestamps.length) % timestamps.length;
    }

    private void updateStatistics() {
        if (size < 2) {
            if (unknownIdeal) {
                cachedAverageIntervalUs = 0;
                cachedMaxIntervalUs = 0;
            } else {
                cachedAverageIntervalUs = idealIntervalUs;
                cachedMaxIntervalUs = idealIntervalUs;
            }
            cachedStability = 0;
            return;
        }
        long sum = 0;
        long max = 0;
        int tail = tail();
        for (int i = 0; i < size - 1; i++) {
            int current = (tail + i) % timestamps.length;
            int next = (tail + i + 1) % timestamps.length;
            long interval = timestamps[next] - timestamps[current];
            sum += interval;
            if (interval > max) {
                max = interval;
            }
        }
        cachedAverageIntervalUs = sum / (size - 1);
        cachedMaxIntervalUs = max;
        long base = unknownIdeal ? cachedAverageIntervalUs : idealIntervalUs;
        if (base == 0) {
            cachedStability = unknownIdeal ? 0 : 1;
            return;
        }
        long deviationSum = 0;
        for (int i = 0; i < size - 1; i++) {
            int current = (tail + i) % timestamps.length;
            int next = (tail + i + 1) % timestamps.length;
            long interval = timestamps[next] - timestamps[current];
            deviationSum += Math.abs(interval - base);
        }
        cachedStability = 1 - (double) deviationSum / (size - 1) / base;
    }

    public synchronized long averageIntervalUs() {
        return cachedAverageIntervalUs;
    }

    public synchronized double averageDeviationPercent() {
        long base = unknownIdeal ? cachedAverageIntervalUs : idealIntervalUs;
        if (base == 0) {
            return 0.0;
        }
        return (double) (cachedAverageIntervalUs - base) / base * 100.0;
    }

    public synchronized long maxIntervalUs() {
        return cachedMaxIntervalUs;
    }

    public synchronized double stability() {
        return cachedStability;
    }

    public synchronized void reset() {
        head = 0;
        size = 0;
        if (unknownIdeal) {
            cachedAverageIntervalUs = 0;
            cachedMaxIntervalUs = 0;
        } else {
            cachedAverageIntervalUs = idealIntervalUs;
            cachedMaxIntervalUs = idealIntervalUs;
        }
    }

    public synchronized void setIdealIntervalMs(double idealIntervalMs) {
        this.idealIntervalUs = (long) (idealIntervalMs * 1000);
        this.timestamps = new long[(int) (windowUs / idealIntervalUs) * 2];
        head = 0;
        size = 0;
        cachedAverageIntervalUs = idealIntervalUs;
        cachedMaxIntervalUs = idealIntervalUs;
    }

    @Override
    public synchronized String toString() {
        double avgMs = cachedAverageIntervalUs / 1000d;
        double maxMs = cachedMaxIntervalUs / 1000d;
        double stbDevPercent = (1 - cachedStability) * 100d;
        if (unknownIdeal) {
            double maxDev = deviationPercent(cachedMaxIntervalUs, cachedAverageIntervalUs);
            return "avg §b%.2fms§r (±%s%.2f%%§r)    max %s%.2fms§r    stb %s%.1f%%§r".formatted(
                    avgMs,
                    colorByDeviation(maxDev), maxDev,
                    colorByDeviation(maxDev), maxMs,
                    colorByDeviation(stbDevPercent),
                    cachedStability * 100
            );
        }
        double avgDev = deviationPercent(cachedAverageIntervalUs, idealIntervalUs);
        double maxDev = deviationPercent(cachedMaxIntervalUs, idealIntervalUs);
        return "avg %s%.2fms§r (±%s%.2f%%§r)    max %s%.2fms§r    stb %s%.1f%%§r".formatted(
                colorByDeviation(avgDev), avgMs,
                colorByDeviation(avgDev), avgDev,
                colorByDeviation(maxDev), maxMs,
                colorByDeviation(stbDevPercent),
                cachedStability * 100
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

    private double deviationPercent(long intervalUs, long baseUs) {
        if (baseUs == 0) {
            return 0;
        }
        return Math.abs((double) (intervalUs - baseUs) / baseUs * 100);
    }
}
