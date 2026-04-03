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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SystemInfoTest {

    @ParameterizedTest
    @CsvSource({
        "0, 0 B",
        "512, 512 B",
        "1023, 1023 B"
    })
    void formatBytes_lessThan1KiB_returnsBytes(long bytes, String expected) {
        String result = SystemInfo.formatBytes(bytes);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
        "1024, 1 KiB",
        "1536, 2 KiB",
        "10240, 10 KiB",
        "1048575, 1024 KiB"
    })
    void formatBytes_between1KiBAnd1MiB_returnsKiB(long bytes, String expected) {
        String result = SystemInfo.formatBytes(bytes);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
        "1048576, 1 MiB",
        "1572864, 2 MiB",
        "10485760, 10 MiB",
        "104857600, 100 MiB",
        "1073741823, 1024 MiB"
    })
    void formatBytes_between1MiBAnd1GiB_returnsMiB(long bytes, String expected) {
        String result = SystemInfo.formatBytes(bytes);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
        "1073741824, 1.0 GiB",
        "2147483648, 2.0 GiB",
        "4294967296, 4.0 GiB",
        "8589934592, 8.0 GiB",
        "17179869184, 16.0 GiB"
    })
    void formatBytes_atOrAbove1GiB_returnsGiB(long bytes, String expected) {
        String result = SystemInfo.formatBytes(bytes);
        assertEquals(expected, result);
    }

    @Test
    void formatBytes_withFractionalGiB_includesOneDecimalPlace() {
        String result = SystemInfo.formatBytes(1610612736L);
        assertTrue(result.contains("GiB"));
        assertTrue(result.contains("."));
        assertTrue(result.endsWith(" GiB"));
    }

    @ParameterizedTest
    @CsvSource({
        "8589934592, 8.0 GiB",
        "9663676416, 9.0 GiB",
        "10737418240, 10.0 GiB",
        "12884901888, 12.0 GiB"
    })
    void formatBytes_withWholeNumberGiB_displaysCorrectly(long bytes, String expected) {
        String result = SystemInfo.formatBytes(bytes);
        assertEquals(expected, result);
    }

    @Test
    void formatBytes_veryLargeValue_returnsGiB() {
        long bytes = 17179869184L;
        String result = SystemInfo.formatBytes(bytes);
        assertTrue(result.contains("GiB"));
    }

    @Test
    void formatBytes_preciseValues() {
        assertEquals("1 KiB", SystemInfo.formatBytes(1024));
        assertEquals("1 MiB", SystemInfo.formatBytes(1048576));
        assertEquals("1.0 GiB", SystemInfo.formatBytes(1073741824));
    }
}
