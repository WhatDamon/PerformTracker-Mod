package org.damon233.performtrackermod.data;

/**
 * Immutable record representing performance metrics at a point in time.
 * Contains FPS (client), TPS (server), and MSPT (server).
 *
 * <p>All fields are immutable once created, making this class thread-safe
 * and suitable for concurrent access.
 *
 * @author PerformTracker
 * @since 1.0.0
 */
public record PerformanceMetrics(
    /** Frames per second (client-side rendering) */
    double fps,
    /** Ticks per second (server tick rate, normally 20) */
    double tps,
    /** Milliseconds per tick (server processing time per tick) */
    double mspt
) {
    /** Empty metrics representing no data collected yet. */
    public static final PerformanceMetrics EMPTY = new PerformanceMetrics(0.0, 0.0, 0.0);

    /**
     * Creates a new PerformanceMetrics instance.
     *
     * @param fps  the frames per second
     * @param tps  the ticks per second
     * @param mspt the milliseconds per tick
     */
    public PerformanceMetrics {
        // Ensure non-negative values
        fps = Math.max(0.0, fps);
        tps = Math.max(0.0, tps);
        mspt = Math.max(0.0, mspt);
    }

    /**
     * Returns a formatted string for chat display.
     *
     * @return a string like "FPS: 60.0 | TPS: 20.00 | MSPT: 0.50"
     */
    public String toChatString() {
        return String.format("FPS: %.1f | TPS: %.2f | MSPT: %.2f", fps, tps, mspt);
    }

    /**
     * Returns a short status indicator based on performance levels.
     *
     * @return "OK" if TPS >= 19, "LAG" if TPS >= 15, "CRITICAL" otherwise
     */
    public String getStatusIndicator() {
        if (tps >= 19.0) {
            return "OK";
        } else if (tps >= 15.0) {
            return "LAG";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * Returns whether the server is experiencing lag.
     *
     * @return true if TPS is below 19
     */
    public boolean isLagging() {
        return tps < 19.0;
    }

    /**
     * Returns whether the server performance is critical.
     *
     * @return true if TPS is below 15
     */
    public boolean isCritical() {
        return tps < 15.0;
    }

    /**
     * Returns FPS value formatted for CSV.
     */
    public double getFps() {
        return fps;
    }

    /**
     * Returns TPS value formatted for CSV.
     */
    public double getTps() {
        return tps;
    }

    /**
     * Returns MSPT value formatted for CSV.
     */
    public double getMspt() {
        return mspt;
    }
}
