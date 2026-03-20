package org.damon233.performtrackermod.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class TranslationService {
    private static final String PREFIX = "performtracker.";

    public static MutableText get(String key) {
        return Text.translatable(PREFIX + key);
    }

    public static MutableText get(String key, Object... args) {
        return Text.translatable(PREFIX + key, args);
    }

    public static Supplier<Text> getSupplier(String key) {
        return () -> get(key);
    }

    public static Supplier<Text> getSupplier(String key, Object... args) {
        return () -> get(key, args);
    }

    public static MutableText chat(String key) {
        return Text.translatable(PREFIX + "chat.prefix").append(" ").append(get(key));
    }

    public static MutableText chat(String key, Object... args) {
        return Text.translatable(PREFIX + "chat.prefix").append(" ").append(get(key, args));
    }

    public static MutableText chatWithMetrics(String metricsText) {
        return Text.translatable(PREFIX + "chat.prefix").append(" ").append(Text.literal(metricsText));
    }

    public static String getStatusKey(boolean ok, boolean critical) {
        if (ok) {
            return "status.ok";
        } else if (critical) {
            return "status.critical";
        } else {
            return "status.lag";
        }
    }
}
