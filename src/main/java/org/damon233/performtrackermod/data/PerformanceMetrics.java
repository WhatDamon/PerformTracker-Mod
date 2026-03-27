package org.damon233.performtrackermod.data;

import org.damon233.performtrackermod.config.ConfigAccess;

public record PerformanceMetrics(
    double fps,
    double tps,
    double mspt,
    double heapUsed,
    double heapMax
) {
    public static final PerformanceMetrics EMPTY = new PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0);

    public PerformanceMetrics {
        fps = Math.max(0.0, fps);
        tps = Math.max(0.0, tps);
        mspt = Math.max(0.0, mspt);
        heapUsed = Math.max(0.0, heapUsed);
        heapMax = Math.max(0.0, heapMax);
    }

    public static String formatValue(double value) {
        if (Double.isInfinite(value)) {
            return "\u221E";
        }
        return String.format("%.2f", value);
    }
    
    public static String formatMemoryMB(double mb) {
        if (ConfigAccess.isBinaryUnits()) {
            if (mb >= 1024) {
                return String.format("%.1fGiB", mb / 1024);
            }
            return String.format("%.0fMiB", mb);
        } else {
            if (mb >= 1000) {
                return String.format("%.1fGB", mb / 1000);
            }
            return String.format("%.0fMB", mb);
        }
    }
}
