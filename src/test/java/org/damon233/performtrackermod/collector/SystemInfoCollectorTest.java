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

package org.damon233.performtrackermod.collector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemInfoCollectorTest {

    private static final String DUMMY_SHA = "0000000000000000000000000000000000000000000000000000000000000000";

    @BeforeEach
    void setUp() {
        SystemInfoCollector.releaseChipRules();
        SystemInfoCollector.loadChipRules();
    }

    @AfterEach
    void tearDown() {
        SystemInfoCollector.releaseChipRules();
    }

    @Test
    void loadChipRules_loadsPhoneAndServerChips() {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt", DUMMY_SHA);

        assertFalse(phoneChips.isEmpty(), "Phone chips list should not be empty");
        assertFalse(serverChips.isEmpty(), "Server chips list should not be empty");
    }

    @Test
    void loadChipList_ignoresCommentsAndEmptyLines() {
        List<String> chips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);

        assertFalse(chips.stream().anyMatch(c -> c.startsWith("#")), "Should not contain comments");
        assertFalse(chips.contains(""), "Should not contain empty strings");
    }

    @Test
    void loadChipList_convertsToUpperCase() {
        List<String> chips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);

        assertTrue(chips.stream().allMatch(c -> c.equals(c.toUpperCase())), "All chips should be uppercase");
    }

    @ParameterizedTest
    @CsvSource({
        "SNAPDRAGON 8, true",
        "SDM8, true",
        "MT68, true",
        "HELIO, true",
        "EXYNOS, true",
        "KIRIN, true",
        "TENSOR, true"
    })
    void matchesAnyChip_phoneChips_matchesCorrectly(String cpuName, boolean expected) {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);

        assertEquals(expected, SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), phoneChips),
            "Should match phone chip: " + cpuName);
    }

    @ParameterizedTest
    @CsvSource({
        "GRAVITON, true",
        "NEOVERSE, true",
        "ALTRA, true",
        "THUNDERX, true",
        "KUNPENG, true",
        "PHYTIUM, true",
        "FT2000, true"
    })
    void matchesAnyChip_serverChips_matchesCorrectly(String cpuName, boolean expected) {
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt", DUMMY_SHA);

        assertEquals(expected, SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), serverChips),
            "Should match server chip: " + cpuName);
    }

    @ParameterizedTest
    @CsvSource({
        "Unknown CPU, false",
        "Random Brand XYZ, false",
        "Generic Chipset, false"
    })
    void matchesAnyChip_noMatch_returnsFalse(String cpuName, boolean expected) {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt", DUMMY_SHA);

        boolean result = SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), phoneChips) ||
                        SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), serverChips);
        assertEquals(expected, result);
    }

    @Test
    void matchesAnyChip_partialMatch_works() {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);

        assertTrue(SystemInfoCollector.matchesAnyChip("Qualcomm SNAPDRAGON 8 Gen 3", phoneChips));
        assertTrue(SystemInfoCollector.matchesAnyChip("MT6800", phoneChips));
    }

    @Test
    void releaseChipRules_clearsChips() {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);
        assertFalse(phoneChips.isEmpty());

        SystemInfoCollector.releaseChipRules();

        SystemInfoCollector.loadChipRules();
        List<String> newPhoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt", DUMMY_SHA);
        assertFalse(newPhoneChips.isEmpty());
    }

    @Test
    void loadChipList_nonexistentPath_returnsEmptyList() {
        List<String> chips = SystemInfoCollector.loadChipList("/nonexistent/chips.txt", DUMMY_SHA);
        assertTrue(chips.isEmpty());
    }

    @Test
    void isChipRulesValid_withValidSha_returnsTrue() {
        SystemInfoCollector.releaseChipRules();
        SystemInfoCollector.loadChipRules();
        assertTrue(SystemInfoCollector.isChipRulesValid());
    }
}
