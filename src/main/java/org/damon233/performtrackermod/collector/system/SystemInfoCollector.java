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

package org.damon233.performtrackermod.collector.system;

import net.fabricmc.loader.api.FabricLoader;
import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.data.DeviceType;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.collector.IGpuProvider;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class SystemInfoCollector {
    private static SystemInfo cachedInfo;
    private static long cachedPhysicalMemory = -1;

    public static SystemInfo collect() {
        if (cachedInfo != null) {
            return cachedInfo;
        }

        DeviceType deviceType = DeviceClassifier.classifyDevice();
        String cpuName = CpuNameDetector.getCpuName();
        String gpuName = getGpuName();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long totalMemoryBytes = getPhysicalMemory();

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String jvmName = System.getProperty("java.vm.name");
        String minecraftVersion = getMinecraftVersion();
        String modVersion = getModVersion();
        String deviceModel = DeviceModelDetector.getDeviceModel();

        cachedInfo = new SystemInfo(
            deviceType,
            deviceType.getCode(),
            cpuName,
            gpuName,
            cpuCores,
            totalMemoryBytes,
            osName,
            osVersion,
            osArch,
            javaVersion,
            jvmName,
            minecraftVersion,
            modVersion,
            deviceModel,
            getJvmArgs()
        );

        return cachedInfo;
    }

    private static String getGpuName() {
        IGpuProvider provider = PerformTracker.getGpuProvider();
        if (provider != null) {
            String name = provider.getGpuName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "Unknown";
    }

    private static long getPhysicalMemory() {
        if (cachedPhysicalMemory > 0) {
            return cachedPhysicalMemory;
        }

        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalMemory = osBean.getTotalMemorySize();
            if (totalMemory > 0) {
                cachedPhysicalMemory = totalMemory;
                return cachedPhysicalMemory;
            }
        } catch (Exception ignored) {
        }

        cachedPhysicalMemory = Runtime.getRuntime().maxMemory();
        return cachedPhysicalMemory;
    }

    private static String[] getJvmArgs() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> sanitized = new ArrayList<>();
        for (String arg : args) {
            sanitized.add(sanitizeJvmArg(arg));
        }
        return sanitized.toArray(new String[0]);
    }

    private static String sanitizeJvmArg(String arg) {
        String lower = arg.toLowerCase();
        String[] sensitivePatterns = {"token", "password", "secret", "auth", "key", "credential"};
        for (String pattern : sensitivePatterns) {
            if (lower.contains(pattern)) {
                int eqIndex = arg.indexOf('=');
                if (eqIndex > 0) {
                    return arg.substring(0, eqIndex + 1) + "***";
                }
                return arg.split("\\s+")[0] + " ***";
            }
        }
        return arg;
    }

    private static String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
    }

    private static String getModVersion() {
        return FabricLoader.getInstance().getModContainer("performtracker")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
    }

    public static void releaseCache() {
        cachedInfo = null;
    }

    public static boolean isChipRulesValid() {
        return ChipRulesManager.isChipRulesValid();
    }

    public static void releaseChipRules() {
        ChipRulesManager.releaseChipRules();
    }
}
