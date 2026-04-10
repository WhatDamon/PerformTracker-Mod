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
import java.net.URL;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChipRulesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");

    private static List<String> phoneChips;
    private static List<String> serverChips;
    private static final AtomicBoolean chipWarningLogged = new AtomicBoolean(false);
    private static volatile boolean chipRulesValid = true;
    private static volatile boolean chipRulesUsedForClassification = false;

    private static final String PHONE_CHIPS_SHA = "428b078c2ba8395b84f2dd2a7ed964ee80feef685ecc6335e9679606ec63162a";
    private static final String SERVER_CHIPS_SHA = "ee3468d6b04970750967004ca71ec0339126fbef69df8c8ea397e023098c473c";

    public static boolean matchesAnyChip(String cpuUpper, List<String> chips) {
        for (String chip : chips) {
            if (cpuUpper.contains(chip)) {
                return true;
            }
        }
        return false;
    }

    public static void releaseChipRules() {
        if (phoneChips != null) {
            phoneChips.clear();
            phoneChips = null;
        }
        if (serverChips != null) {
            serverChips.clear();
            serverChips = null;
        }
        chipWarningLogged.set(false);
        chipRulesValid = true;
        chipRulesUsedForClassification = false;
    }

    public static void loadChipRules() {
        if (phoneChips != null && serverChips != null) {
            return;
        }

        phoneChips = loadChipList("/assets/performtracker/phone_chips.txt", PHONE_CHIPS_SHA);
        serverChips = loadChipList("/assets/performtracker/server_chips.txt", SERVER_CHIPS_SHA);
    }

    public static boolean isChipRulesValid() {
        if (!chipRulesUsedForClassification) {
            return true;
        }
        return chipRulesValid;
    }

    public static void setChipRulesUsed(boolean used) {
        chipRulesUsedForClassification = used;
    }

    public static List<String> getPhoneChips() {
        return phoneChips;
    }

    public static List<String> getServerChips() {
        return serverChips;
    }

    public static List<String> loadChipList(String path, String expectedSha) {
        List<String> list = new java.util.ArrayList<>();
        try {
            URL resource = ChipRulesManager.class.getResource(path);
            if (resource == null) {
                LOGGER.warn("Chip rules file not found: {}", path);
                chipRulesValid = false;
                return list;
            }

            try (InputStream is = resource.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] rawBytes = baos.toByteArray();

                String computedSha = computeSha256(rawBytes);
                if (!computedSha.equalsIgnoreCase(expectedSha)) {
                    if (chipWarningLogged.compareAndSet(false, true)) {
                        LOGGER.warn("Chip rules file '{}' SHA mismatch. Expected: {}, Got: {}. "
                            + "The file may have been modified incorrectly.", path, expectedSha, computedSha);
                    }
                    chipRulesValid = false;
                }

                String content = CharsetDetector.decode(rawBytes);
                for (String line : content.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    list.add(line.toUpperCase());
                }
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
