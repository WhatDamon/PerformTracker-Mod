package org.damon233.performtrackermod.collector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.damon233.performtrackermod.utils.ThreadSafeCounter;

/**
 * Collects server tick performance metrics including TPS (Ticks Per Second)
 * and MSPT (Milliseconds Per Tick).
 *
 * <p>This collector uses a rolling window of the last 100 tick durations to
 * calculate accurate, recent performance metrics. Thread-safe implementation
 * using atomic operations.
 *
 * @author PerformTracker
 * @since 1.0.0
 */
public class ServerTickCollector {
    /** Rolling window size for tick duration tracking */
    private static final int WINDOW_SIZE = 100;

    /** Target TPS when server is running optimally */
    private static final double TARGET_TPS = 20.0;

    /** Conversion factor from nanoseconds to milliseconds */
    private static final double NANOS_TO_MILLIS = 1_000_000.0;

    /** Conversion factor from nanoseconds to seconds */
    private static final double NANOS_TO_SECONDS = 1_000_000_000.0;

    /** Circular buffer storing tick durations in nanoseconds */
    private final long[] tickDurations = new long[WINDOW_SIZE];

    /** Current position in the circular buffer */
    private final AtomicInteger bufferIndex = new AtomicInteger(0);

    /** Number of ticks recorded (max WINDOW_SIZE) */
    private final AtomicInteger tickCount = new AtomicInteger(0);

    /** Running sum of tick durations in the window (nanoseconds) */
    private final AtomicLong durationSum = new AtomicLong(0);

    /** Whether metrics collection is enabled */
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    /** Timestamp when current tick started */
    private volatile long tickStartTime = 0;
    
    /** Whether we have recorded a valid tick (to skip first-tick anomaly) */
    private volatile boolean hasValidTick = false;
    
    /** Expected tick duration in milliseconds (50ms for 20 TPS) */
    private static final double EXPECTED_TICK_MS = 50.0;
    
    /** Max acceptable tick duration before considering it a pause/resume (3x expected) */
    private static final double MAX_ACCEPTABLE_TICK_MS = EXPECTED_TICK_MS * 3;

    /** Total tick duration counter (for long-term average fallback) */
    private final ThreadSafeCounter totalDurationCounter = new ThreadSafeCounter();

    /** Number of ticks since last reset */
    private final ThreadSafeCounter tickCounter = new ThreadSafeCounter();

    /**
     * Creates a new ServerTickCollector and registers the tick event callback.
     */
    public ServerTickCollector() {
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    /**
     * Callback invoked at the end of each server tick.
     *
     * @param server the Minecraft server instance
     */
    private void onEndServerTick(net.minecraft.server.MinecraftServer server) {
        if (!enabled.get()) {
            return;
        }

        // Calculate tick duration
        long currentTime = System.nanoTime();
        long tickDuration = currentTime - tickStartTime;
        double tickDurationMs = tickDuration / NANOS_TO_MILLIS;

        // Skip outliers (likely pause/resume) - don't record abnormally long ticks
        if (tickDurationMs > MAX_ACCEPTABLE_TICK_MS) {
            tickStartTime = currentTime;
            return;
        }

        // Store in circular buffer
        int index = bufferIndex.getAndIncrement() % WINDOW_SIZE;
        long oldDuration = tickDurations[index];
        tickDurations[index] = tickDuration;

        durationSum.addAndGet(tickDuration - oldDuration);

        // Increment tick count (capped at WINDOW_SIZE)
        int currentCount;
        do {
            currentCount = tickCount.get();
            if (currentCount >= WINDOW_SIZE) {
                break;
            }
        } while (!tickCount.compareAndSet(currentCount, currentCount + 1));

        totalDurationCounter.add(tickDuration);
        tickCounter.add(1);

        // Record start time for next tick
        tickStartTime = currentTime;
        hasValidTick = true;
    }

    /**
     * Returns the current TPS (Ticks Per Second).
     *
     * <p>TPS is calculated as: {@code TPS = Math.min(20.0, 1000.0 / MSPT)}
     * When MSPT is approximately 50ms, TPS should be around 20.0.
     *
     * @return the current TPS value (0.0 if no ticks recorded)
     */
    public double getTps() {
        double mspt = getMspt();
        if (mspt <= 0.0) {
            return 0.0;
        }
        return Math.min(TARGET_TPS, 1000.0 / mspt);
    }

    public double getMspt() {
        if (!hasValidTick) {
            return 1000.0 / TARGET_TPS;
        }
        
        int count = tickCount.get();
        if (count == 0) {
            return 1000.0 / TARGET_TPS;
        }

        long sum = durationSum.get();
        int actualCount = Math.min(count, WINDOW_SIZE);
        double windowMspt = (sum / actualCount) / NANOS_TO_MILLIS;

        double tpsBasedMspt = 1000.0 / getTpsFromWindow(actualCount, sum);

        if (windowMspt > tpsBasedMspt * 1.5 && tpsBasedMspt > 0) {
            return tpsBasedMspt;
        }

        return windowMspt;
    }
    
    private double getTpsFromWindow(int count, long sum) {
        if (count == 0 || sum == 0) {
            return TARGET_TPS;
        }
        double mspt = (sum / count) / NANOS_TO_MILLIS;
        if (mspt <= 0) {
            return TARGET_TPS;
        }
        return Math.min(TARGET_TPS, 1000.0 / mspt);
    }

    /**
     * Returns whether metrics collection is enabled.
     *
     * @return true if collection is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Enables or disables metrics collection.
     *
     * @param enabled true to enable collection, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    /**
     * Resets all collected metrics to their initial state.
     *
     * <p>This clears the rolling window and all counters. The enabled
     * state is preserved.
     */
    public void reset() {
        // Clear the buffer
        for (int i = 0; i < WINDOW_SIZE; i++) {
            tickDurations[i] = 0;
        }

        // Reset counters
        bufferIndex.set(0);
        tickCount.set(0);
        durationSum.set(0);
        totalDurationCounter.reset();
        tickCounter.reset();
        tickStartTime = System.nanoTime();
        hasValidTick = false;
    }

    /**
     * Returns the number of ticks currently in the rolling window.
     *
     * @return the number of ticks recorded (0 to WINDOW_SIZE)
     */
    public int getTickCount() {
        return tickCount.get();
    }

    /**
     * Returns the size of the rolling window.
     *
     * @return the maximum number of ticks tracked
     */
    public int getWindowSize() {
        return WINDOW_SIZE;
    }
}
