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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class WindowsVersionTypeTest {

    @ParameterizedTest
    @CsvSource({
        "5, 1, 0, WINDOWS_XP",
        "5, 2, 0, WINDOWS_SERVER_2003",
        "6, 0, 0, WINDOWS_VISTA",
        "6, 1, 0, WINDOWS_7",
        "6, 2, 0, WINDOWS_8",
        "6, 3, 0, WINDOWS_8_1"
    })
    void fromNtVersion_preWindows10_returnsCorrectType(int major, int minor, int build, WindowsVersionType expected) {
        assertEquals(expected, WindowsVersionType.fromNtVersion(major, minor, build));
    }

    @ParameterizedTest
    @CsvSource({
        "10, 0, 0, WINDOWS_10",
        "10, 0, 10240, WINDOWS_10",
        "10, 0, 14393, WINDOWS_10",
        "10, 0, 19045, WINDOWS_10",
        "10, 0, 21996, WINDOWS_11",
        "10, 0, 22631, WINDOWS_11",
        "10, 0, 26100, WINDOWS_11"
    })
    void fromNtVersion_windows10And11_detectsCorrectly(int major, int minor, int build, WindowsVersionType expected) {
        assertEquals(expected, WindowsVersionType.fromNtVersion(major, minor, build));
    }

    @Test
    void fromNtVersion_unknownVersion_returnsUnknown() {
        assertEquals(WindowsVersionType.UNKNOWN, WindowsVersionType.fromNtVersion(4, 0, 0));
        assertEquals(WindowsVersionType.UNKNOWN, WindowsVersionType.fromNtVersion(0, 0, 0));
    }

    @Test
    void isWindows11_onlyWindows11_returnsTrue() {
        assertTrue(WindowsVersionType.WINDOWS_11.isWindows11());
    }

    @ParameterizedTest
    @CsvSource({
        "WINDOWS_XP",
        "WINDOWS_SERVER_2003",
        "WINDOWS_VISTA",
        "WINDOWS_7",
        "WINDOWS_8",
        "WINDOWS_8_1",
        "WINDOWS_10",
        "UNKNOWN"
    })
    void isWindows11_otherTypes_returnsFalse(WindowsVersionType type) {
        assertFalse(type.isWindows11());
    }

    @ParameterizedTest
    @CsvSource({
        "WINDOWS_XP, Windows XP",
        "WINDOWS_SERVER_2003, Windows Server 2003",
        "WINDOWS_VISTA, Windows Vista",
        "WINDOWS_7, Windows 7",
        "WINDOWS_8, Windows 8",
        "WINDOWS_8_1, Windows 8.1",
        "WINDOWS_10, Windows 10",
        "WINDOWS_11, Windows 11",
        "UNKNOWN, Unknown"
    })
    void getProductName_returnsCorrectName(WindowsVersionType type, String expectedName) {
        assertEquals(expectedName, type.getProductName());
    }
}
