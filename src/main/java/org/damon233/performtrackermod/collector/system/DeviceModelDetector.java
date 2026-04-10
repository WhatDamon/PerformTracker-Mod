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

import org.damon233.performtrackermod.utils.platform.linux.LinuxDeviceModelDetector;
import org.damon233.performtrackermod.utils.platform.macos.MacDeviceModelDetector;
import org.damon233.performtrackermod.utils.platform.windows.WindowsDeviceModelDetector;

public class DeviceModelDetector {
    private static String cachedDeviceModel;

    public static String getDeviceModel() {
        if (cachedDeviceModel != null) {
            return cachedDeviceModel;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            cachedDeviceModel = MacDeviceModelDetector.getDeviceModel();
        } else if (osName.contains("linux")) {
            cachedDeviceModel = LinuxDeviceModelDetector.getDeviceModel();
        } else if (osName.contains("windows")) {
            cachedDeviceModel = WindowsDeviceModelDetector.getDeviceModel();
        } else {
            cachedDeviceModel = "Unknown";
        }

        if (cachedDeviceModel.equalsIgnoreCase("To Be Filled By O.E.M.") || cachedDeviceModel.equalsIgnoreCase("Default String")) {
            cachedDeviceModel = "Unknown";
        }

        return cachedDeviceModel;
    }
}
