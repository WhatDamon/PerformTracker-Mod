package org.damon233.performtrackermod.collector;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ServerMetricsCollector {
    private static final double TARGET_TPS = 20.0;
    private static final double NANOS_TO_SECONDS = 1_000_000_000.0;
    private static final long WINDOW_SECONDS = 5;
    private static final long WINDOW_NANOS = WINDOW_SECONDS * 1_000_000_000L;

    private final ConcurrentLinkedDeque<Long> tickTimes = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public ServerMetricsCollector() {
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
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

    public void reset() {
        tickTimes.clear();
    }
}
