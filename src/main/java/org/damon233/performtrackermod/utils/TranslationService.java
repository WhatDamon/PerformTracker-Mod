package org.damon233.performtrackermod.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class TranslationService {
    private static final String PREFIX = "performtracker.";
    private static final String RESET = "\u00A7r";
    private static final String GOLD = "\u00A76";
    private static final String GREEN = "\u00A7a";
    private static final String RED = "\u00A7c";
    private static final String AQUA = "\u00A7b";
    private static final String GRAY = "\u00A77";
    
    private static final String CHAT_PREFIX = GOLD + "[⚓]" + RESET;

    public static MutableText get(String key) {
        return Text.translatable(PREFIX + key);
    }

    public static MutableText get(String key, Object... args) {
        return Text.translatable(PREFIX + key, args);
    }

    public static MutableText chat(String key) {
        return Text.literal(CHAT_PREFIX + " ").append(get(key));
    }

    public static MutableText chat(String key, Object... args) {
        return Text.literal(CHAT_PREFIX + " ").append(get(key, args));
    }
    
    public static MutableText chatSuccess(String key) {
        return Text.literal(CHAT_PREFIX + " " + GREEN).append(get(key)).append(RESET);
    }
    
    public static MutableText chatSuccess(String key, Object... args) {
        return Text.literal(CHAT_PREFIX + " " + GREEN).append(get(key, args)).append(RESET);
    }
    
    public static MutableText chatError(String key) {
        return Text.literal(CHAT_PREFIX + " " + RED).append(get(key)).append(RESET);
    }
    
    public static MutableText chatError(String key, Object... args) {
        return Text.literal(CHAT_PREFIX + " " + RED).append(get(key, args)).append(RESET);
    }

    public static MutableText chatWithMetrics(String metricsText) {
        return Text.literal(CHAT_PREFIX + " ").append(Text.literal(metricsText));
    }
    
    public static String colorValue(String value) {
        return AQUA + value + RESET;
    }
    
    public static String colorLabel(String label) {
        return GRAY + label + RESET;
    }
}
