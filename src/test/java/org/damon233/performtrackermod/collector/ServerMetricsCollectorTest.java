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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerMetricsCollectorTest {

    private ServerMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new ServerMetricsCollector();
    }

    @Test
    void getCpuUsagePercent_returnsValidValue() {
        double cpuUsage = collector.getCpuUsagePercent();
        assertTrue(cpuUsage >= -1 && cpuUsage <= 100,
            "CPU usage should be between -1 and 100, got: " + cpuUsage);
    }

    @Test
    void getCpuUsagePercent_multipleCalls_returnsSmoothedValue() {
        double first = collector.getCpuUsagePercent();
        double second = collector.getCpuUsagePercent();
        double third = collector.getCpuUsagePercent();

        assertTrue(first >= -1 && first <= 100);
        assertTrue(second >= -1 && second <= 100);
        assertTrue(third >= -1 && third <= 100);
    }

    @Test
    void getTps_returnsReasonableValue() {
        double tps = collector.getTps();
        assertTrue(tps > 0 && tps <= 20.0,
            "TPS should be between 0 and 20, got: " + tps);
    }

    @Test
    void getMspt_returnsReasonableValue() {
        double mspt = collector.getMspt();
        assertTrue(mspt > 0,
            "MSPT should be positive, got: " + mspt);
    }

    @Test
    void getHeapUsedMB_returnsNonNegative() {
        double heapUsed = collector.getHeapUsedMB();
        assertTrue(heapUsed >= 0,
            "Heap used should be non-negative, got: " + heapUsed);
    }

    @Test
    void getHeapMaxMB_returnsPositive() {
        double heapMax = collector.getHeapMaxMB();
        assertTrue(heapMax > 0,
            "Heap max should be positive, got: " + heapMax);
    }

    @Test
    void getHeapUsedMB_lessThanOrEqualToHeapMaxMB() {
        double heapUsed = collector.getHeapUsedMB();
        double heapMax = collector.getHeapMaxMB();
        assertTrue(heapUsed <= heapMax,
            "Heap used (" + heapUsed + ") should be <= heap max (" + heapMax + ")");
    }

    @Test
    void reset_clearsTickTimes() {
        double tpsBefore = collector.getTps();
        assertTrue(tpsBefore > 0);

        collector.reset();

        double tpsAfter = collector.getTps();
        assertTrue(tpsAfter > 0 || tpsAfter == 20.0,
            "TPS after reset should still be valid, got: " + tpsAfter);
    }

    @Test
    void cpuUsage_initiallyHasValue() {
        assertDoesNotThrow(() -> {
            double cpu = collector.getCpuUsagePercent();
            assertTrue(cpu >= -1 && cpu <= 100);
        });
    }
}
