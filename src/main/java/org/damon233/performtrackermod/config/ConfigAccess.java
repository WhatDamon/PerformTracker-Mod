package org.damon233.performtrackermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String CONFIG_FILE_NAME = "performtracker.json";
    
    private static final int DEFAULT_OUTPUT_INTERVAL_SECONDS = 5;
    private static final boolean DEFAULT_CHAT_ENABLED = true;
    private static final boolean DEFAULT_CSV_ENABLED = true;
    private static final String DEFAULT_CSV_DIRECTORY = "performance_data";
    private static final boolean DEFAULT_NETWORK_ENABLED = false;
    private static final String DEFAULT_NETWORK_URL = "http://localhost:31415/api/metrics";
    private static final boolean DEFAULT_COLLECT_FPS = true;
    private static final boolean DEFAULT_COLLECT_TPS = true;
    private static final boolean DEFAULT_COLLECT_MSPT = true;
    
    private static ConfigData configData;
    private static boolean initialized = false;
    
    private static class ConfigData {
        int outputIntervalSeconds = DEFAULT_OUTPUT_INTERVAL_SECONDS;
        boolean chatEnabled = DEFAULT_CHAT_ENABLED;
        boolean csvEnabled = DEFAULT_CSV_ENABLED;
        String csvDirectory = DEFAULT_CSV_DIRECTORY;
        boolean networkEnabled = DEFAULT_NETWORK_ENABLED;
        String networkUrl = DEFAULT_NETWORK_URL;
        boolean collectFps = DEFAULT_COLLECT_FPS;
        boolean collectTps = DEFAULT_COLLECT_TPS;
        boolean collectMspt = DEFAULT_COLLECT_MSPT;
    }
    
    /**
     * Initialize config - must be called on client startup.
     * Safe to call multiple times.
     */
    @Environment(EnvType.CLIENT)
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        
        // Load existing config or create default
        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile);
                Gson gson = new Gson();
                ConfigData loaded = gson.fromJson(content, ConfigData.class);
                if (loaded != null) {
                    configData = loaded;
                    // Ensure all fields exist in old configs
                    if (configData.networkUrl == null) {
                        configData.networkUrl = DEFAULT_NETWORK_URL;
                    }
                    LOGGER.info("Loaded config from {}", configFile);
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read config file", e);
            }
        }
        
        // Use defaults
        configData = new ConfigData();
        LOGGER.info("Using default config");
        save();
    }
    
    /**
     * Save config to file.
     */
    @Environment(EnvType.CLIENT)
    private static void save() {
        if (configData == null) {
            return;
        }
        
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String content = gson.toJson(configData);
            Files.writeString(configFile, content);
            LOGGER.debug("Saved config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
    
    public static int getOutputIntervalSeconds() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.outputIntervalSeconds : DEFAULT_OUTPUT_INTERVAL_SECONDS;
        }
        return DEFAULT_OUTPUT_INTERVAL_SECONDS;
    }
    
    public static int getOutputIntervalTicks() {
        return getOutputIntervalSeconds() * 20;
    }
    
    public static boolean isChatEnabled() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.chatEnabled : DEFAULT_CHAT_ENABLED;
        }
        return DEFAULT_CHAT_ENABLED;
    }
    
    public static boolean isCsvEnabled() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.csvEnabled : DEFAULT_CSV_ENABLED;
        }
        return DEFAULT_CSV_ENABLED;
    }
    
    public static String getCsvDirectory() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.csvDirectory : DEFAULT_CSV_DIRECTORY;
        }
        return DEFAULT_CSV_DIRECTORY;
    }
    
    public static boolean isNetworkEnabled() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.networkEnabled : DEFAULT_NETWORK_ENABLED;
        }
        return DEFAULT_NETWORK_ENABLED;
    }
    
    public static String getNetworkUrl() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.networkUrl : DEFAULT_NETWORK_URL;
        }
        return DEFAULT_NETWORK_URL;
    }
    
    public static boolean isCollectFps() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.collectFps : DEFAULT_COLLECT_FPS;
        }
        return DEFAULT_COLLECT_FPS;
    }
    
    public static boolean isCollectTps() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.collectTps : DEFAULT_COLLECT_TPS;
        }
        return DEFAULT_COLLECT_TPS;
    }
    
    public static boolean isCollectMspt() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return configData != null ? configData.collectMspt : DEFAULT_COLLECT_MSPT;
        }
        return DEFAULT_COLLECT_MSPT;
    }
    
    @Environment(EnvType.CLIENT)
    public static void setOutputIntervalSeconds(int seconds) {
        if (configData != null) {
            configData.outputIntervalSeconds = Math.max(1, Math.min(3600, seconds));
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setChatEnabled(boolean enabled) {
        if (configData != null) {
            configData.chatEnabled = enabled;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCsvEnabled(boolean enabled) {
        if (configData != null) {
            configData.csvEnabled = enabled;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCsvDirectory(String directory) {
        if (configData != null) {
            configData.csvDirectory = (directory != null && !directory.isBlank()) ? directory : DEFAULT_CSV_DIRECTORY;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setNetworkEnabled(boolean enabled) {
        if (configData != null) {
            configData.networkEnabled = enabled;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setNetworkUrl(String url) {
        if (configData != null) {
            configData.networkUrl = (url != null && !url.isBlank()) ? url : DEFAULT_NETWORK_URL;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCollectFps(boolean enabled) {
        if (configData != null) {
            configData.collectFps = enabled;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCollectTps(boolean enabled) {
        if (configData != null) {
            configData.collectTps = enabled;
            save();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void setCollectMspt(boolean enabled) {
        if (configData != null) {
            configData.collectMspt = enabled;
            save();
        }
    }
    
    public static boolean isClothConfigLoaded() {
        return FabricLoader.getInstance().isModLoaded("cloth-config");
    }
}
