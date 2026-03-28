package org.damon233.performtrackermod.data;

public record SystemInfo(
    DeviceType deviceType,
    String deviceTypeDisplay,
    String cpuName,
    String gpuName,
    int cpuCores,
    long totalMemoryBytes,
    String osName,
    String osVersion,
    String osArch,
    String javaVersion,
    String jvmName,
    String minecraftVersion,
    String modVersion
) {
    public static String formatBytes(long bytes) {
        if (bytes >= 1073741824) {
            return String.format("%.1f GiB", bytes / 1073741824.0);
        }
        if (bytes >= 1048576) {
            return String.format("%.0f MiB", bytes / 1048576.0);
        }
        if (bytes >= 1024) {
            return String.format("%.0f KiB", bytes / 1024.0);
        }
        return bytes + " B";
    }
}
