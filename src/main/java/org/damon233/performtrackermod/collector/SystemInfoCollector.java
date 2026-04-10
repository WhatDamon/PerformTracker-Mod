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

import net.fabricmc.loader.api.FabricLoader;
import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.data.DeviceType;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.utils.CharsetDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemInfoCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");

    private static SystemInfo cachedInfo;
    private static List<String> phoneChips;
    private static List<String> serverChips;
    private static final AtomicBoolean chipWarningLogged = new AtomicBoolean(false);
    private static volatile boolean chipRulesValid = true;
    private static volatile boolean chipRulesUsedForClassification = false;

    // Permanent caches - CPU info and memory never change at runtime
    private static String cachedCpuName;
    private static long cachedPhysicalMemory = -1;
    private static String cachedDeviceModel;

    // SHA-256 checksums for chip rules files (update when file content changes)
    private static final String PHONE_CHIPS_SHA = "428b078c2ba8395b84f2dd2a7ed964ee80feef685ecc6335e9679606ec63162a";
    private static final String SERVER_CHIPS_SHA = "ee3468d6b04970750967004ca71ec0339126fbef69df8c8ea397e023098c473c";

    public static SystemInfo collect() {
        if (cachedInfo != null) {
            return cachedInfo;
        }

        DeviceType deviceType = classifyDevice();
        String cpuName = getCpuName();
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
            getDeviceModel()
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

    private static String getCpuName() {
        if (cachedCpuName != null) {
            return cachedCpuName;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            cachedCpuName = getWindowsCpuName();
        } else if (osName.contains("linux")) {
            cachedCpuName = getLinuxCpuName();
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            cachedCpuName = getMacCpuName();
        } else {
            cachedCpuName = System.getProperty("os.arch");
        }

        return cachedCpuName;
    }

    private static String getWindowsCpuName() {
        String[] commands = {
            "powershell -NoProfile -Command \"Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name\"",
            "powershell -NoProfile -Command \"(Get-WmiObject Win32_Processor).Name\""
        };

        for (String command : commands) {
            String result = runCommand(command);
            if (result != null && !result.trim().isEmpty()) {
                return result.lines().findFirst().orElse(result).trim();
            }
        }

        return System.getProperty("os.arch");
    }

    private static String getLinuxCpuName() {
        String output = readFile("/proc/cpuinfo");
        if (output == null) {
            return "Unknown";
        }

        for (String line : output.split("\n")) {
            line = line.trim();

            if (line.startsWith("model name") || line.startsWith("Model name")) {
                return line.split(":", 2)[1].trim();
            }

            if (line.startsWith("Hardware") || line.startsWith("Processor")) {
                return line.split(":", 2)[1].trim();
            }
        }

        return System.getProperty("os.arch");
    }

    private static String getMacCpuName() {
        String result = runCommand("sysctl -n machdep.cpu.brand_string");
        if (result != null && !result.isBlank()) {
            return result.trim();
        }
        return System.getProperty("os.arch");
    }

    private static String getDeviceModel() {
        if (cachedDeviceModel != null) {
            return cachedDeviceModel;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            cachedDeviceModel = getMacDeviceModel();
        } else if (osName.contains("linux")) {
            cachedDeviceModel = getLinuxDeviceModel();
        } else if (osName.contains("windows")) {
            cachedDeviceModel = getWindowsDeviceModel();
        } else {
            cachedDeviceModel = "Unknown";
        }

        if (cachedDeviceModel.equalsIgnoreCase("To Be Filled By O.E.M.") || cachedDeviceModel.equalsIgnoreCase("Default String")) {
            cachedDeviceModel = "Unknown";
        }

        return cachedDeviceModel;
    }

    private static String getMacDeviceModel() {
        String result = runCommand("sysctl -n hw.model");
        return result != null ? result.trim() : "Unknown";
    }

    private static String getLinuxDeviceModel() {
        String model = readFile("/sys/devices/virtual/dmi/id/product_name");
        if (model != null && !model.isBlank()) {
            return model.trim();
        }

        model = readFile("/proc/device-tree/model");
        if (model != null && !model.isBlank()) {
            return model.trim();
        }

        return "Unknown";
    }

    private static String getWindowsDeviceModel() {
        String[] commands = {
            "powershell -NoProfile -Command \"(Get-CimInstance Win32_ComputerSystem).Model\"",
            "powershell -NoProfile -Command \"(Get-WmiObject Win32_ComputerSystem).Model\"",
            "cmd /c for /f \"tokens=2 delims==\" %A in ('wmic computersystem get model /value') do @echo %A"
        };

        for (String command : commands) {
            String result = runCommand(command);
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        }

        return "Unknown";
    }

    private static String runCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            process.waitFor();

            return CharsetDetector.decode(baos.toByteArray()).trim();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String readFile(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(path));
            return CharsetDetector.decode(bytes);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static long getPhysicalMemory() {
        if (cachedPhysicalMemory > 0) {
            return cachedPhysicalMemory;
        }

        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
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

    private static DeviceType classifyDevice() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            return DeviceType.MAC;
        }

        boolean isArm = osArch.contains("arm") || osArch.contains("aarch64");
        if (osName.contains("linux") && isArm) {
            String cpuName = getCpuName();
            String gpuName = getGpuName();
            return classifyArmDevice(cpuName, gpuName);
        }

        return DeviceType.PC;
    }

    private static DeviceType classifyArmDevice(String cpuInfo, String gpuInfo) {
        chipRulesUsedForClassification = true;
        loadChipRules();

        String upper = cpuInfo != null ? cpuInfo.toUpperCase() : "";
        String gpuUpper = gpuInfo != null ? gpuInfo.toUpperCase() : "";
        DeviceType result;

        if (upper.contains("APPLE")) {
            result = DeviceType.MAC;
        } else if (gpuUpper.contains("APPLE")) {
            result = DeviceType.PHONE;
        } else if (upper.isEmpty() || cpuInfo == null || cpuInfo.equals("Unknown")) {
            result = DeviceType.EMB;
        } else if (matchesAnyChip(upper, phoneChips)) {
            result = DeviceType.PHONE;
        } else if (matchesAnyChip(upper, serverChips)) {
            result = DeviceType.PC;
        } else {
            result = DeviceType.EMB;
        }

        return result;
    }

    static boolean matchesAnyChip(String cpuUpper, List<String> chips) {
        for (String chip : chips) {
            if (cpuUpper.contains(chip)) {
                return true;
            }
        }
        return false;
    }

    static void releaseChipRules() {
        if (phoneChips != null) {
            phoneChips.clear();
            phoneChips = null;
        }
        if (serverChips != null) {
            serverChips.clear();
            serverChips = null;
        }
        chipWarningLogged.set(false);
        chipRulesValid = true;
        chipRulesUsedForClassification = false;
    }

    public static void releaseCache() {
        cachedInfo = null;
    }

    static void loadChipRules() {
        if (phoneChips != null && serverChips != null) {
            return;
        }

        phoneChips = loadChipList("/assets/performtracker/phone_chips.txt", PHONE_CHIPS_SHA);
        serverChips = loadChipList("/assets/performtracker/server_chips.txt", SERVER_CHIPS_SHA);
    }

    public static boolean isChipRulesValid() {
        if (!chipRulesUsedForClassification) {
            return true;
        }
        return chipRulesValid;
    }

    static List<String> loadChipList(String path, String expectedSha) {
        List<String> list = new java.util.ArrayList<>();
        try {
            URL resource = SystemInfoCollector.class.getResource(path);
            if (resource == null) {
                LOGGER.warn("Chip rules file not found: {}", path);
                chipRulesValid = false;
                return list;
            }

            try (InputStream is = resource.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] rawBytes = baos.toByteArray();

                String computedSha = computeSha256(rawBytes);
                if (!computedSha.equalsIgnoreCase(expectedSha)) {
                    if (chipWarningLogged.compareAndSet(false, true)) {
                        LOGGER.warn("Chip rules file '{}' SHA mismatch. Expected: {}, Got: {}. "
                            + "The file may have been modified incorrectly.", path, expectedSha, computedSha);
                    }
                    chipRulesValid = false;
                }

                String content = CharsetDetector.decode(rawBytes);
                for (String line : content.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    list.add(line.toUpperCase());
                }
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
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
}
