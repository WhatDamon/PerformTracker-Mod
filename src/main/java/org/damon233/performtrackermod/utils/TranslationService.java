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

package org.damon233.performtrackermod.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TranslationService {
    private static final String PREFIX = "performtracker.";

    private static final MutableText PREFIX_TEXT = Text.literal("[").withColor(getColorValue(Formatting.DARK_GRAY))
            .append(Text.literal("\uD83E\uDE9D").withColor(getColorValue(Formatting.GOLD)))
            .append(Text.literal("] ").withColor(getColorValue(Formatting.DARK_GRAY)));

    private static int getColorValue(Formatting formatting) {
        Integer color = formatting.getColorValue();
        return color != null ? color : 0xFFFFFF;
    }

    public static MutableText get(String key) {
        return Text.translatable(PREFIX + key);
    }

    public static MutableText get(String key, Object... args) {
        return Text.translatable(PREFIX + key, args);
    }

    public static MutableText chat(String key) {
        return Text.literal("").append(PREFIX_TEXT).append(get(key));
    }

    public static MutableText chat(String key, Object... args) {
        return Text.literal("").append(PREFIX_TEXT).append(get(key, args));
    }

    public static MutableText chatSuccess(String key) {
        return Text.literal("").append(PREFIX_TEXT).append(Text.literal("").withColor(getColorValue(Formatting.GREEN))).append(get(key));
    }

    public static MutableText chatSuccess(String key, Object... args) {
        return Text.literal("").append(PREFIX_TEXT).append(Text.literal("").withColor(getColorValue(Formatting.GREEN))).append(get(key, args));
    }

    public static MutableText chatError(String key) {
        return Text.literal("").append(PREFIX_TEXT).append(Text.literal("").withColor(getColorValue(Formatting.RED))).append(get(key));
    }

    public static MutableText chatError(String key, Object... args) {
        return Text.literal("").append(PREFIX_TEXT).append(Text.literal("").withColor(getColorValue(Formatting.RED))).append(get(key, args));
    }

    public static MutableText chatWithMetrics(MutableText metricsText) {
        return Text.literal("").append(PREFIX_TEXT).append(metricsText);
    }

    public static MutableText colorValue(String value) {
        return Text.literal(value).withColor(0x75B5FF).formatted(Formatting.BOLD);
    }

    public static MutableText colorLabel(String label) {
        return Text.literal(label).withColor(getColorValue(Formatting.GRAY));
    }
}
