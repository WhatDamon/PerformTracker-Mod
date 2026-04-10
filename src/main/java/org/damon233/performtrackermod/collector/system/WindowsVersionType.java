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

public enum WindowsVersionType {
    WINDOWS_XP("Windows XP", 5, 1),
    WINDOWS_SERVER_2003("Windows Server 2003", 5, 2),
    WINDOWS_VISTA("Windows Vista", 6, 0),
    WINDOWS_7("Windows 7", 6, 1),
    WINDOWS_8("Windows 8", 6, 2),
    WINDOWS_8_1("Windows 8.1", 6, 3),
    WINDOWS_10("Windows 10", 10, 0),
    WINDOWS_11("Windows 11", 10, 0, 21996),
    UNKNOWN("Unknown", -1, -1);

    private final String productName;
    private final int majorVersion;
    private final int minorVersion;
    private final int minBuild;

    WindowsVersionType(String productName, int majorVersion, int minorVersion) {
        this(productName, majorVersion, minorVersion, -1);
    }

    WindowsVersionType(String productName, int majorVersion, int minorVersion, int minBuild) {
        this.productName = productName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.minBuild = minBuild;
    }

    public String getProductName() {
        return productName;
    }

    public boolean isWindows11() {
        return this == WINDOWS_11;
    }

    public static WindowsVersionType fromNtVersion(int major, int minor, int build) {
        if (major == 10 && minor == 0) {
            return build >= 21996 ? WINDOWS_11 : WINDOWS_10;
        }

        for (WindowsVersionType type : values()) {
            if (type == WINDOWS_10 || type == WINDOWS_11 || type == UNKNOWN) {
                continue;
            }
            if (type.majorVersion == major && type.minorVersion == minor) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
