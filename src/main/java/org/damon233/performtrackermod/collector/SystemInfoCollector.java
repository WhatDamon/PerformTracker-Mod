package org.damon233.performtrackermod.collector;

import net.fabricmc.loader.api.FabricLoader;
import org.damon233.performtrackermod.data.DeviceType;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.PerformTracker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.damon233.performtrackermod.utils.CharsetDetector;

public class SystemInfoCollector {
    private static SystemInfo cachedInfo;

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
        String modVersion = FabricLoader.getInstance().getModContainer("performtracker")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("Unknown");

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
            String result = getWindowsCpuName();
            if (result != null) return result;
            return System.getProperty("os.arch");
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
        
        return null;
    }
    
    private static String getLinuxCpuName() {
        String output = runCommand("cat /proc/cpuinfo");
        if (output == null) return null;
        
        for (String line : output.split("\n")) {
            if (line.startsWith("model name") || line.startsWith("Processor") || line.startsWith("Hardware")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    return line.substring(colon + 1).trim();
                }
            }
        }
        return null;
    }
    
    private static String getMacCpuName() {
        return runCommand("sysctl -n machdep.cpu.brand_string");
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

    private static long getPhysicalMemory() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("linux")) {
            try {
                String meminfo = readFile("/proc/meminfo");
                for (String line : meminfo.split("\n")) {
                    if (line.startsWith("MemTotal:")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            return Long.parseLong(parts[1]) * 1024;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        try {
            Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            Object osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (clazz.isInstance(osBean)) {
                java.lang.reflect.Method method = clazz.getMethod("getTotalMemorySize");
                Object result = method.invoke(osBean);
                if (result instanceof Number) {
                    return ((Number) result).longValue();
                }
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

        if (osName.contains("windows") || 
            (osName.contains("linux") && (osArch.contains("amd64") || osArch.contains("x86")))) {
            return DeviceType.PC;
        }

        if (osName.contains("linux") && (osArch.contains("arm") || osArch.contains("aarch64"))) {
            String cpuInfo = getCpuName();
            return classifyArmDevice(cpuInfo);
        }

        return DeviceType.PC;
    }

    private static DeviceType classifyArmDevice(String cpuInfo) {
        if (cpuInfo == null) return DeviceType.EMB;
        
        String upper = cpuInfo.toUpperCase();
        
        if (upper.contains("BCM") || upper.contains("BROADCOM")) {
            return DeviceType.EMB;
        }
        
        if (upper.contains("APPLE")) {
            return DeviceType.MAC;
        }
        
        if (upper.contains("QUALCOMM") || upper.contains("SNAPDRAGON") ||
            upper.contains("MEDIATEK") || upper.contains("DIMENSITY") ||
            upper.contains("EXYNOS") ||
            upper.contains("KIRIN") ||
            upper.contains("TENSOR") ||
            upper.contains("UNISOC")) {
            return DeviceType.PHONE;
        }
        
        return DeviceType.EMB;
    }

    private static String readFile(String path) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null && content.length() < 5000) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static String getMinecraftVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("Unknown");
        } catch (Exception ignored) {
        }
        return "Unknown";
    }
}
