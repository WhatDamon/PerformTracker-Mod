package org.damon233.performtrackermod.collector;

import net.fabricmc.loader.api.FabricLoader;
import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.data.DeviceType;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.utils.CharsetDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SystemInfoCollector {
    private static SystemInfo cachedInfo;
    private static List<String> phoneChips;
    private static List<String> serverChips;

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
            modVersion
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
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            return getWindowsCpuName();
        }
        if (osName.contains("linux")) {
            return getLinuxCpuName();
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return getMacCpuName();
        }
        return System.getProperty("os.arch");
    }

    private static String getWindowsCpuName() {
        String[] commands = {
            "powershell -NoProfile -Command Get-CimInstance -ClassName Win32_Processor | Select-Object -ExpandProperty Name",
            "powershell -NoProfile -Command (Get-WmiObject Win32_Processor).Name"
        };

        for (String command : commands) {
            String result = runCommand(command);
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        }

        return System.getProperty("os.arch");
    }

    private static String getLinuxCpuName() {
        String output = readFile("/proc/cpuinfo");
        if (output == null) return "Unknown";

        String[] prefixes = {"model name", "Model name", "Processor", "Hardware", "model", "Model"};
        for (String prefix : prefixes) {
            for (String line : output.split("\n")) {
                if (line.startsWith(prefix)) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        return line.substring(colon + 1).trim();
                    }
                }
            }
        }
        return "Unknown";
    }

    private static String getMacCpuName() {
        String result = runCommand("sysctl -n machdep.cpu.brand_string");
        return result != null ? result.trim() : System.getProperty("os.arch");
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
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            String meminfo = readFile("/proc/meminfo");
            if (meminfo != null) {
                for (String line : meminfo.split("\n")) {
                    if (line.startsWith("MemTotal:")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                return Long.parseLong(parts[1]) * 1024;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            java.lang.reflect.Method method = osBean.getClass().getMethod("getTotalMemorySize");
            Object result = method.invoke(osBean);
            if (result instanceof Number num && num.longValue() > 0) {
                return num.longValue();
            }
        } catch (Exception ignored) {
        }

        return Runtime.getRuntime().maxMemory();
    }

    private static DeviceType classifyDevice() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            return DeviceType.MAC;
        }

        boolean isArm = osArch.contains("arm") || osArch.contains("aarch64");
        if (osName.contains("linux") && isArm) {
            return classifyArmDevice(getCpuName());
        }

        if (osName.contains("windows") ||
            (osName.contains("linux") && (osArch.contains("amd64") || osArch.contains("x86")))) {
            return DeviceType.PC;
        }

        return DeviceType.PC;
    }

    private static DeviceType classifyArmDevice(String cpuInfo) {
        loadChipRules();

        DeviceType result;
        if (cpuInfo == null || cpuInfo.equals("Unknown")) {
            result = DeviceType.EMB;
        } else {
            String upper = cpuInfo.toUpperCase();

            if (upper.contains("APPLE")) {
                result = DeviceType.MAC;
            } else if (matchesAnyChip(upper, phoneChips)) {
                result = DeviceType.PHONE;
            } else if (matchesAnyChip(upper, serverChips)) {
                result = DeviceType.PC;
            } else {
                result = DeviceType.EMB;
            }
        }

        releaseChipRules();
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
    }

    static void loadChipRules() {
        if (phoneChips != null && serverChips != null) {
            return;
        }

        phoneChips = loadChipList("/assets/performtracker/phone_chips.txt");
        serverChips = loadChipList("/assets/performtracker/server_chips.txt");
    }

    static List<String> loadChipList(String path) {
        List<String> list = new java.util.ArrayList<>();
        try {
            URL resource = SystemInfoCollector.class.getResource(path);
            if (resource == null) {
                return list;
            }

            try (InputStream is = resource.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String content = CharsetDetector.decode(baos.toByteArray());

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
