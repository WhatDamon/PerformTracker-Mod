package org.damon233.performtrackermod.collector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemInfoCollectorTest {

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
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt");

        assertFalse(phoneChips.isEmpty(), "Phone chips list should not be empty");
        assertFalse(serverChips.isEmpty(), "Server chips list should not be empty");
    }

    @Test
    void loadChipList_ignoresCommentsAndEmptyLines() {
        List<String> chips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");

        assertFalse(chips.stream().anyMatch(c -> c.startsWith("#")), "Should not contain comments");
        assertFalse(chips.contains(""), "Should not contain empty strings");
    }

    @Test
    void loadChipList_convertsToUpperCase() {
        List<String> chips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");

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
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");

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
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt");

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
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");
        List<String> serverChips = SystemInfoCollector.loadChipList("/assets/performtracker/server_chips.txt");

        boolean result = SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), phoneChips) ||
                        SystemInfoCollector.matchesAnyChip(cpuName.toUpperCase(), serverChips);
        assertEquals(expected, result);
    }

    @Test
    void matchesAnyChip_partialMatch_works() {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");

        assertTrue(SystemInfoCollector.matchesAnyChip("Qualcomm SNAPDRAGON 8 Gen 3", phoneChips));
        assertTrue(SystemInfoCollector.matchesAnyChip("MT6800", phoneChips));
    }

    @Test
    void releaseChipRules_clearsChips() {
        List<String> phoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");
        assertFalse(phoneChips.isEmpty());

        SystemInfoCollector.releaseChipRules();

        SystemInfoCollector.loadChipRules();
        List<String> newPhoneChips = SystemInfoCollector.loadChipList("/assets/performtracker/phone_chips.txt");
        assertFalse(newPhoneChips.isEmpty());
    }

    @Test
    void loadChipList_nonexistentPath_returnsEmptyList() {
        List<String> chips = SystemInfoCollector.loadChipList("/nonexistent/chips.txt");
        assertTrue(chips.isEmpty());
    }
}
