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

package org.damon233.performtrackermod.utils.platform.linux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LinuxOSInfoDetector {
    private static Map<String, String> osRelease;

    public static String getOSReleaseName() {
        loadOsRelease();
        return osRelease.get("NAME");
    }

    public static String getOSReleasePrettyName() {
        loadOsRelease();
        return osRelease.get("PRETTY_NAME");
    }

    private static void loadOsRelease() {
        if (osRelease != null) {
            return;
        }

        osRelease = new HashMap<>();
        Path osReleaseFile = Path.of("/etc/os-release");

        if (Files.exists(osReleaseFile)) {
            try {
                for (String line : Files.readString(osReleaseFile).split("\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIndex = line.indexOf('=');
                    if (eqIndex > 0) {
                        String key = line.substring(0, eqIndex);
                        String value = line.substring(eqIndex + 1);
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        osRelease.put(key, value);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
