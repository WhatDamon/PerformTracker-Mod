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

import org.damon233.performtrackermod.utils.CommandExecutor;
import org.damon233.performtrackermod.utils.platform.PlatformDetector;

public class CpuNameDetector {
    private static String cachedCpuName;

    public static String getCpuName() {
        if (cachedCpuName != null) {
            return cachedCpuName;
        }

        if (PlatformDetector.isWindows()) {
            cachedCpuName = getWindowsCpuName();
        } else if (PlatformDetector.isLinux()) {
            cachedCpuName = getLinuxCpuName();
        } else if (PlatformDetector.isMac()) {
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
            String result = CommandExecutor.runCommand(command);
            if (result != null && !result.trim().isEmpty()) {
                return result.lines().findFirst().orElse(result).trim();
            }
        }

        return System.getProperty("os.arch");
    }

    private static String getLinuxCpuName() {
        String output = CommandExecutor.readFile("/proc/cpuinfo");
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
        String result = CommandExecutor.runCommand("sysctl -n machdep.cpu.brand_string");
        if (result != null && !result.isBlank()) {
            return result.trim();
        }
        return System.getProperty("os.arch");
    }
}
