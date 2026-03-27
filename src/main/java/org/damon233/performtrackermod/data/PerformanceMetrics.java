package org.damon233.performtrackermod.data;

public record PerformanceMetrics(
    double fps,
    double tps,
    double mspt
) {
    public static final PerformanceMetrics EMPTY = new PerformanceMetrics(0.0, 0.0, 0.0);

    public PerformanceMetrics {
        fps = Math.max(0.0, fps);
        tps = Math.max(0.0, tps);
        mspt = Math.max(0.0, mspt);
    }

    private static String formatValue(double value) {
        if (Double.isInfinite(value)) {
            return "\u221E";
        }
        return String.format("%.2f", value);
    }

    public String toChatString() {
        return String.format("FPS: %.1f | TPS: %s | MSPT: %s", 
            fps, formatValue(tps), formatValue(mspt));
    }
}
