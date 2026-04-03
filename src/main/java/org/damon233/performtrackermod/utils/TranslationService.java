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
