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

import org.damon233.performtrackermod.utils.CharsetDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CpuNameDetector {
    private static String cachedCpuName;

    public static String getCpuName() {
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

    public static String runCommand(String command) {
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

    static String readFile(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(path));
            return CharsetDetector.decode(bytes);
        } catch (IOException ignored) {
            return null;
        }
    }
}
