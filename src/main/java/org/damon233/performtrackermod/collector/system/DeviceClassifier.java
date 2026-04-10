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

import org.damon233.performtrackermod.data.DeviceType;

public class DeviceClassifier {
    public static DeviceType classifyDevice() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            return DeviceType.MAC;
        }

        boolean isArm = osArch.contains("arm") || osArch.contains("aarch64");
        if (osName.contains("linux") && isArm) {
            String cpuName = CpuNameDetector.getCpuName();
            String gpuName = getGpuName();
            return classifyArmDevice(cpuName, gpuName);
        }

        return DeviceType.PC;
    }

    private static String getGpuName() {
        try {
            Class<?> providerClass = Class.forName("org.damon233.performtrackermod.PerformTracker");
            java.lang.reflect.Method getGpuProvider = providerClass.getMethod("getGpuProvider");
            Object provider = getGpuProvider.invoke(null);
            if (provider != null) {
                java.lang.reflect.Method getGpuName = provider.getClass().getMethod("getGpuName");
                String name = (String) getGpuName.invoke(provider);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    private static DeviceType classifyArmDevice(String cpuInfo, String gpuInfo) {
        ChipRulesManager.setChipRulesUsed(true);
        ChipRulesManager.loadChipRules();

        String upper = cpuInfo != null ? cpuInfo.toUpperCase() : "";
        String gpuUpper = gpuInfo != null ? gpuInfo.toUpperCase() : "";

        DeviceType result;
        if (upper.contains("APPLE")) {
            result = DeviceType.MAC;
        } else if (gpuUpper.contains("APPLE")) {
            result = DeviceType.PHONE;
        } else if (upper.isEmpty() || cpuInfo == null || cpuInfo.equals("Unknown")) {
            result = DeviceType.EMB;
        } else if (ChipRulesManager.matchesAnyChip(upper, ChipRulesManager.getPhoneChips())) {
            result = DeviceType.PHONE;
        } else if (ChipRulesManager.matchesAnyChip(upper, ChipRulesManager.getServerChips())) {
            result = DeviceType.PC;
        } else {
            result = DeviceType.EMB;
        }

        ChipRulesManager.releaseChipRules();
        return result;
    }
}
