package org.damon233.performtrackermod.collector;

public class ServerMetricsCollector {
    private final TickTracker tickTracker;
    private final CpuMetrics cpuMetrics;

    public ServerMetricsCollector() {
        this.tickTracker = new TickTracker();
        this.cpuMetrics = new CpuMetrics();
    }

    public double getTps() {
        return tickTracker.getTps();
    }

    public double getMspt() {
        return tickTracker.getMspt();
    }

    public double getHeapUsedMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
    }

    public double getHeapMaxMB() {
        return Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
    }

    public double getCpuUsagePercent() {
        return cpuMetrics.getCpuUsagePercent();
    }

    public void reset() {
        tickTracker.reset();
    }
}
