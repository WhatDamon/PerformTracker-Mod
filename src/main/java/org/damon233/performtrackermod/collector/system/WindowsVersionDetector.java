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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsVersionDetector {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern OS_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.?(\\d*)");

    public static String getWindowsVersion() {
        String fallback = System.getProperty("os.version");

        int major = 0, minor = 0, build = 0, revision = 0;

        Matcher fallbackMatcher = OS_VERSION_PATTERN.matcher(fallback);
        if (fallbackMatcher.find()) {
            try {
                major = Integer.parseInt(fallbackMatcher.group(1));
                minor = Integer.parseInt(fallbackMatcher.group(2));
                if (fallbackMatcher.group(3) != null && !fallbackMatcher.group(3).isEmpty()) {
                    build = Integer.parseInt(fallbackMatcher.group(3));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String currentBuild = queryRegistry("CurrentBuild");
        if (currentBuild != null) {
            try {
                build = Integer.parseInt(currentBuild.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (major >= 10) {
            String ubr = queryRegistry("UBR");
            if (ubr != null) {
                try {
                    revision = Integer.parseInt(ubr.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (build == 0) {
            String cmdVersion = getVersionFromCmdVer();
            if (cmdVersion != null) {
                return cmdVersion;
            }
        }

        WindowsVersionType versionType = WindowsVersionType.fromNtVersion(major, minor, build);

        if (build > 0) {
            if (versionType == WindowsVersionType.WINDOWS_11) {
                if (revision > 0) {
                    return String.format("10.0.%d.%d (%s)", build, revision, versionType.getProductName());
                } else {
                    return String.format("10.0.%d (%s)", build, versionType.getProductName());
                }
            } else if (versionType == WindowsVersionType.WINDOWS_10) {
                if (revision > 0) {
                    return String.format("10.0.%d.%d (%s)", build, revision, versionType.getProductName());
                } else {
                    return String.format("10.0.%d (%s)", build, versionType.getProductName());
                }
            } else if (versionType == WindowsVersionType.UNKNOWN) {
                if (revision > 0) {
                    return String.format("%d.%d.%d.%d", major, minor, build, revision);
                } else {
                    return String.format("%d.%d.%d", major, minor, build);
                }
            } else {
                if (revision > 0) {
                    return String.format("%d.%d.%d.%d (%s)", major, minor, build, revision, versionType.getProductName());
                } else {
                    return String.format("%d.%d.%d (%s)", major, minor, build, versionType.getProductName());
                }
            }
        }

        return fallback;
    }

    private static String queryRegistry(String valueName) {
        String command = "reg query \"" + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion" + "\" /v " + valueName;
        String result = CpuNameDetector.runCommand(command);
        if (result == null) {
            return null;
        }

        String[] lines = result.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(valueName)) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    return parts[parts.length - 1];
                }
            }
        }
        return null;
    }

    private static String getVersionFromCmdVer() {
        String result = CpuNameDetector.runCommand("cmd /c ver");
        if (result == null) {
            return null;
        }

        Matcher matcher = VERSION_PATTERN.matcher(result);
        if (matcher.find()) {
            try {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int build = Integer.parseInt(matcher.group(3));
                int revision = Integer.parseInt(matcher.group(4));
                return String.format("%d.%d.%d.%d", major, minor, build, revision);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
