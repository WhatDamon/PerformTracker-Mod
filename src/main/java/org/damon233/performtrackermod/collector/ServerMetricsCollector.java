/*
 * Copyright 2026 Damon Lu and open-source contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
