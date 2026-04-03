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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicReference;

public class CpuMetrics {
    private static final double CPU_SMOOTHING_FACTOR = 0.3;

    private final OperatingSystemMXBean osBean;
    private final AtomicReference<Double> smoothedCpuUsage;
    private volatile boolean warmedUp = false;

    public CpuMetrics() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.smoothedCpuUsage = new AtomicReference<>(0.0);
        startWarmUpAsync();
    }

    private void startWarmUpAsync() {
        Thread.ofVirtual().name("PerformTracker-CpuWarmup").start(() -> {
            for (int i = 0; i < 10; i++) {
                double raw = getRawCpuUsage();
                if (raw >= 0) {
                    smoothedCpuUsage.set(raw);
                    warmedUp = true;
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            warmedUp = true;
        });
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

    public double getCpuUsagePercent() {
        if (!warmedUp) {
            return -1;
        }

        double rawUsage = getRawCpuUsage();
        if (rawUsage < 0) {
            return rawUsage;
        }

        double current = smoothedCpuUsage.get();
        double smoothed = current * (1 - CPU_SMOOTHING_FACTOR) + rawUsage * CPU_SMOOTHING_FACTOR;
        smoothedCpuUsage.set(smoothed);
        return smoothed;
    }
}
