package org.damon233.performtrackermod.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigAccess {
    private static final String MOD_ID = "performtracker";
    
    private static final int DEFAULT_OUTPUT_INTERVAL_SECONDS = 5;
    private static final boolean DEFAULT_CHAT_ENABLED = true;
    private static final boolean DEFAULT_CSV_ENABLED = true;
    private static final String DEFAULT_CSV_DIRECTORY = "performance_data";
    
    private static int cachedIntervalSeconds = DEFAULT_OUTPUT_INTERVAL_SECONDS;
    private static boolean cachedChatEnabled = DEFAULT_CHAT_ENABLED;
    private static boolean cachedCsvEnabled = DEFAULT_CSV_ENABLED;
    private static String cachedCsvDirectory = DEFAULT_CSV_DIRECTORY;
    
    public static int getOutputIntervalSeconds() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return cachedIntervalSeconds;
        }
        return DEFAULT_OUTPUT_INTERVAL_SECONDS;
    }
    
    public static int getOutputIntervalTicks() {
        return getOutputIntervalSeconds() * 20;
    }
    
    public static boolean isChatEnabled() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return cachedChatEnabled;
        }
        return DEFAULT_CHAT_ENABLED;
    }
    
    public static boolean isCsvEnabled() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return cachedCsvEnabled;
        }
        return DEFAULT_CSV_ENABLED;
    }
    
    public static String getCsvDirectory() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return cachedCsvDirectory;
        }
        return DEFAULT_CSV_DIRECTORY;
    }
    
    @Environment(EnvType.CLIENT)
    public static void setOutputIntervalSeconds(int seconds) {
        cachedIntervalSeconds = Math.max(1, Math.min(3600, seconds));
    }
    
    @Environment(EnvType.CLIENT)
    public static void setChatEnabled(boolean enabled) {
        cachedChatEnabled = enabled;
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCsvEnabled(boolean enabled) {
        cachedCsvEnabled = enabled;
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCsvDirectory(String directory) {
        cachedCsvDirectory = (directory != null && !directory.isBlank()) ? directory : DEFAULT_CSV_DIRECTORY;
    }
    
    public static boolean isClothConfigLoaded() {
        return FabricLoader.getInstance().isModLoaded("cloth-config");
    }
}
