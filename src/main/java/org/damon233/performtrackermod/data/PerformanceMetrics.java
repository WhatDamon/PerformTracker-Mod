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

package org.damon233.performtrackermod.data;

import org.damon233.performtrackermod.config.ConfigAccess;

public record PerformanceMetrics(
    double fps,
    double tps,
    double mspt,
    double heapUsed,
    double heapMax,
    double cpuUsage
) {
    public static final PerformanceMetrics EMPTY = new PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0, -1.0);

    public PerformanceMetrics {
        fps = Math.max(0.0, fps);
        tps = Math.max(0.0, tps);
        mspt = Math.max(0.0, mspt);
        heapUsed = Math.max(0.0, heapUsed);
        heapMax = Math.max(0.0, heapMax);
        if (cpuUsage < 0) {
            cpuUsage = -1.0;
        }
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
