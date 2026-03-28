package org.damon233.performtrackermod.collector;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ServerMetricsCollector {
    private static final double TARGET_TPS = 20.0;
    private static final double NANOS_TO_SECONDS = 1_000_000_000.0;
    private static final long WINDOW_SECONDS = 5;
    private static final long WINDOW_NANOS = WINDOW_SECONDS * 1_000_000_000L;
    private static final double CPU_SMOOTHING_FACTOR = 0.3;

    private final ConcurrentLinkedDeque<Long> tickTimes = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final OperatingSystemMXBean osBean;
    private final AtomicReference<Double> smoothedCpuUsage;

    public ServerMetricsCollector() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.smoothedCpuUsage = new AtomicReference<>(0.0);
        warmUpCpu();
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    private void warmUpCpu() {
        for (int i = 0; i < 10; i++) {
            double raw = getRawCpuUsage();
            if (raw >= 0) {
                smoothedCpuUsage.set(raw);
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private double getRawCpuUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double load = sunOsBean.getCpuLoad();
                if (load >= 0) {
                    return load * 100.0;
                }
            }
            long[] ticks = getSystemCpuLoadTicks();
            if (ticks != null && ticks.length >= 4) {
                long total = ticks[0] + ticks[1] + ticks[2] + ticks[3];
                if (total > 0) {
                    return ((double) (total - ticks[3]) / total) * 100.0;
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private long[] getSystemCpuLoadTicks() {
        try {
            java.lang.reflect.Method method = osBean.getClass().getMethod("getSystemCpuLoadTicks");
            method.setAccessible(true);
            return (long[]) method.invoke(osBean);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void onEndServerTick(net.minecraft.server.MinecraftServer server) {
        if (!enabled.get()) {
            return;
        }

        long currentTime = System.nanoTime();
        tickTimes.add(currentTime);

        long cutoff = currentTime - WINDOW_NANOS;
        while (!tickTimes.isEmpty() && tickTimes.peekFirst() < cutoff) {
            tickTimes.pollFirst();
        }
    }

    public double getTps() {
        if (tickTimes.isEmpty()) {
            return TARGET_TPS;
        }

        long first = tickTimes.peekFirst();
        long current = System.nanoTime();
        double elapsedSeconds = (current - first) / NANOS_TO_SECONDS;

        if (elapsedSeconds <= 0) {
            return TARGET_TPS;
        }

        int count = tickTimes.size();
        return Math.min(TARGET_TPS, count / elapsedSeconds);
    }

    public double getMspt() {
        double tps = getTps();
        if (tps <= 0) {
            return 1000.0 / TARGET_TPS;
        }
        return 1000.0 / tps;
    }
    
    public double getHeapUsedMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
    }
    
    public double getHeapMaxMB() {
        return Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
    }
    
    public double getCpuUsagePercent() {
        double rawUsage = getRawCpuUsage();
        if (rawUsage < 0) {
            return rawUsage;
        }
        
        double current = smoothedCpuUsage.get();
        double smoothed = current * (1 - CPU_SMOOTHING_FACTOR) + rawUsage * CPU_SMOOTHING_FACTOR;
        smoothedCpuUsage.set(smoothed);
        return smoothed;
    }

    public void reset() {
        tickTimes.clear();
    }
}
