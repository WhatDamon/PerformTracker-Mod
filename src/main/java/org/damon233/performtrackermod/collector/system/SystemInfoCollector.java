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

import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.data.DeviceType;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.collector.IGpuProvider;
import org.damon233.performtrackermod.utils.CachedValue;
import org.damon233.performtrackermod.utils.ModInfoHelper;
import org.damon233.performtrackermod.utils.platform.PlatformDetector;
import org.damon233.performtrackermod.utils.platform.linux.LinuxOSInfoDetector;
import org.damon233.performtrackermod.utils.platform.windows.WindowsVersionDetector;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class SystemInfoCollector {
    private static final CachedValue<SystemInfo> SYSTEM_INFO = new CachedValue<>(SystemInfoCollector::computeSystemInfo);
    private static final CachedValue<Long> PHYSICAL_MEMORY = new CachedValue<>(SystemInfoCollector::computePhysicalMemory);

    public static SystemInfo collect() {
        return SYSTEM_INFO.get();
    }

    private static SystemInfo computeSystemInfo() {
        DeviceType deviceType = DeviceClassifier.classifyDevice();
        String cpuName = CpuNameDetector.getCpuName();
        String gpuName = getGpuName();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long totalMemoryBytes = PHYSICAL_MEMORY.get();

        String osName = System.getProperty("os.name");
        String osVersion = getOSVersion();
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String jvmName = System.getProperty("java.vm.name");
        String minecraftVersion = getMinecraftVersion();
        String modVersion = getModVersion();
        String deviceModel = DeviceModelDetector.getDeviceModel();

        return new SystemInfo(
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
            getJvmArgs(),
            ModInfoHelper.getModListArray()
        );
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

    private static long computePhysicalMemory() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalMemory = osBean.getTotalMemorySize();
            if (totalMemory > 0) {
                return totalMemory;
            }
        } catch (Exception ignored) {
        }
        return Runtime.getRuntime().maxMemory();
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
        return ModInfoHelper.getModVersion("minecraft");
    }

    private static String getModVersion() {
        return ModInfoHelper.getModVersion("performtracker");
    }

    public static void releaseCache() {
        SYSTEM_INFO.invalidate();
        PHYSICAL_MEMORY.invalidate();
    }

    public static boolean isChipRulesValid() {
        return ChipRulesManager.isChipRulesValid();
    }

    private static String getOSVersion() {
        if (PlatformDetector.isWindows()) {
            return WindowsVersionDetector.getWindowsVersion();
        }
        if (PlatformDetector.isLinux()) {
            String prettyName = LinuxOSInfoDetector.getOSReleasePrettyName();
            if (prettyName != null && !prettyName.isBlank()) {
                return prettyName;
            }
        }
        return System.getProperty("os.version");
    }
}
