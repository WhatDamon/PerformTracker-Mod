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

package org.damon233.performtrackermod.utils.platform.windows;

import org.damon233.performtrackermod.collector.system.CpuNameDetector;

public class WindowsDeviceModelDetector {
    public static String getDeviceModel() {
        String[] commands = {
            "powershell -NoProfile -Command \"(Get-CimInstance Win32_ComputerSystem).Model\"",
            "powershell -NoProfile -Command \"(Get-WmiObject Win32_ComputerSystem).Model\"",
            "cmd /c for /f \"tokens=2 delims==\" %A in ('wmic computersystem get model /value') do @echo %A"
        };

        for (String command : commands) {
            String result = CpuNameDetector.runCommand(command);
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        }

        return "Unknown";
    }
}
