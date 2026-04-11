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
    String modVersion,
    String deviceModel,
    String[] jvmArgs,
    String[] modList
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
