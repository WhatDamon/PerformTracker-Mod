package org.damon233.performtrackermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String CONFIG_FILE_NAME = "performtracker.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final int DEFAULT_OUTPUT_INTERVAL_SECONDS = 5;
    private static final boolean DEFAULT_CHAT_ENABLED = true;
    private static final boolean DEFAULT_CSV_ENABLED = true;
    private static final String DEFAULT_CSV_DIRECTORY = "performance_data";
    private static final boolean DEFAULT_NETWORK_ENABLED = false;
    private static final String DEFAULT_NETWORK_ENDPOINT = "http://localhost:31415";
    private static final String API_PATH = "/api/metrics";
    private static final boolean DEFAULT_COLLECT_FPS = true;
    private static final boolean DEFAULT_COLLECT_TPS = true;
    private static final boolean DEFAULT_COLLECT_MSPT = true;
    private static final boolean DEFAULT_COLLECT_HEAP = true;
    private static final boolean DEFAULT_COLLECT_CPU = true;
    private static final boolean DEFAULT_BINARY_UNITS = true;
    
    private static ConfigData configData;
    private static boolean initialized = false;
    private static Path configFilePath;
    
    private static class ConfigData {
        int outputIntervalSeconds = DEFAULT_OUTPUT_INTERVAL_SECONDS;
        boolean chatEnabled = DEFAULT_CHAT_ENABLED;
        boolean csvEnabled = DEFAULT_CSV_ENABLED;
        String csvDirectory = DEFAULT_CSV_DIRECTORY;
        boolean networkEnabled = DEFAULT_NETWORK_ENABLED;
        String networkEndpoint = DEFAULT_NETWORK_ENDPOINT;
        boolean collectFps = DEFAULT_COLLECT_FPS;
        boolean collectTps = DEFAULT_COLLECT_TPS;
        boolean collectMspt = DEFAULT_COLLECT_MSPT;
        boolean collectHeap = DEFAULT_COLLECT_HEAP;
        boolean collectCpu = DEFAULT_COLLECT_CPU;
        boolean binaryUnits = DEFAULT_BINARY_UNITS;
    }
    
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        configFilePath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        
        if (Files.exists(configFilePath)) {
            try {
                String content = Files.readString(configFilePath);
                ConfigData loaded = GSON.fromJson(content, ConfigData.class);
                if (loaded != null) {
                    configData = loaded;
                    if (configData.networkEndpoint == null) {
                        configData.networkEndpoint = DEFAULT_NETWORK_ENDPOINT;
                    }
                    LOGGER.info("Loaded config from {}", configFilePath);
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read config file", e);
            }
        }
        
        configData = new ConfigData();
        LOGGER.info("Using default config");
        save();
    }
    
    private static void save() {
        if (configData == null || configFilePath == null) {
            return;
        }
        
        try {
            Files.createDirectories(configFilePath.getParent());
            String content = GSON.toJson(configData);
            Files.writeString(configFilePath, content);
            LOGGER.debug("Saved config to {}", configFilePath);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
    
    public static int getOutputIntervalSeconds() {
        return configData != null ? configData.outputIntervalSeconds : DEFAULT_OUTPUT_INTERVAL_SECONDS;
    }
    
    public static boolean isChatEnabled() {
        return configData != null ? configData.chatEnabled : DEFAULT_CHAT_ENABLED;
    }
    
    public static boolean isCsvEnabled() {
        return configData != null ? configData.csvEnabled : DEFAULT_CSV_ENABLED;
    }
    
    public static String getCsvDirectory() {
        return configData != null ? configData.csvDirectory : DEFAULT_CSV_DIRECTORY;
    }
    
    public static boolean isNetworkEnabled() {
        return configData != null ? configData.networkEnabled : DEFAULT_NETWORK_ENABLED;
    }
    
    public static String getNetworkEndpoint() {
        return configData != null ? configData.networkEndpoint : DEFAULT_NETWORK_ENDPOINT;
    }
    
    public static String getNetworkUrl() {
        return getNetworkEndpoint() + API_PATH;
    }
    
    public static String getLocalServerEndpoint() {
        return getNetworkEndpoint();
    }
    
    public static int getLocalServerPort() {
        return parsePort(getNetworkEndpoint());
    }
    
    public static int parsePort(String url) {
        try {
            int start = url.indexOf("://") + 3;
            int colon = url.indexOf(":", start);
            int slash = url.indexOf("/", start);
            if (colon > 0) {
                int end = slash > 0 ? slash : url.length();
                return Integer.parseInt(url.substring(colon + 1, end));
            }
        } catch (Exception ignored) {
        }
        return 31415;
    }
    
    public static boolean isCollectFps() {
        return configData != null ? configData.collectFps : DEFAULT_COLLECT_FPS;
    }
    
    public static boolean isCollectTps() {
        return configData != null ? configData.collectTps : DEFAULT_COLLECT_TPS;
    }
    
    public static boolean isCollectMspt() {
        return configData != null ? configData.collectMspt : DEFAULT_COLLECT_MSPT;
    }
    
    public static boolean isCollectHeap() {
        return configData != null ? configData.collectHeap : DEFAULT_COLLECT_HEAP;
    }
    
    public static boolean isCollectCpu() {
        return configData != null ? configData.collectCpu : DEFAULT_COLLECT_CPU;
    }
    
    public static boolean isBinaryUnits() {
        return configData != null ? configData.binaryUnits : DEFAULT_BINARY_UNITS;
    }
    
    public static void setOutputIntervalSeconds(int seconds) {
        if (configData != null) {
            configData.outputIntervalSeconds = Math.max(1, Math.min(3600, seconds));
            save();
        }
    }
    
    public static void setChatEnabled(boolean enabled) {
        if (configData != null) {
            configData.chatEnabled = enabled;
            save();
        }
    }
    
    public static void setCsvEnabled(boolean enabled) {
        if (configData != null) {
            configData.csvEnabled = enabled;
            save();
        }
    }
    
    public static void setCsvDirectory(String directory) {
        if (configData != null) {
            configData.csvDirectory = (directory != null && !directory.isBlank()) ? directory : DEFAULT_CSV_DIRECTORY;
            save();
        }
    }
    
    public static void setNetworkEnabled(boolean enabled) {
        if (configData != null) {
            configData.networkEnabled = enabled;
            save();
        }
    }
    
    public static void setNetworkEndpoint(String endpoint) {
        if (configData != null) {
            String validated = validateNetworkEndpoint(endpoint);
            configData.networkEndpoint = validated != null ? validated : DEFAULT_NETWORK_ENDPOINT;
            save();
        }
    }
    
    public static String validateNetworkEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        String trimmed = endpoint.trim();
        if (!trimmed.matches("^https?://[\\w\\-]+(\\.[\\w\\-]+)*(:\\d+)?(/.*)?$")) {
            return null;
        }
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }
    
    public static boolean isValidNetworkEndpoint(String endpoint) {
        return validateNetworkEndpoint(endpoint) != null;
    }
    
    public static void setCollectFps(boolean enabled) {
        if (configData != null) {
            configData.collectFps = enabled;
            save();
        }
    }
    
    public static void setCollectTps(boolean enabled) {
        if (configData != null) {
            configData.collectTps = enabled;
            save();
        }
    }
    
    public static void setCollectMspt(boolean enabled) {
        if (configData != null) {
            configData.collectMspt = enabled;
            save();
        }
    }
    
    public static void setCollectHeap(boolean enabled) {
        if (configData != null) {
            configData.collectHeap = enabled;
            save();
        }
    }
    
    public static void setCollectCpu(boolean enabled) {
        if (configData != null) {
            configData.collectCpu = enabled;
            save();
        }
    }
    
    public static void setBinaryUnits(boolean enabled) {
        if (configData != null) {
            configData.binaryUnits = enabled;
            save();
        }
    }
    
    public static boolean isClothConfigLoaded() {
        return FabricLoader.getInstance().isModLoaded("cloth-config");
    }
    
    public static int getDefaultOutputIntervalSeconds() {
        return DEFAULT_OUTPUT_INTERVAL_SECONDS;
    }
    
    public static boolean getDefaultChatEnabled() {
        return DEFAULT_CHAT_ENABLED;
    }
    
    public static boolean getDefaultCsvEnabled() {
        return DEFAULT_CSV_ENABLED;
    }
    
    public static String getDefaultCsvDirectory() {
        return DEFAULT_CSV_DIRECTORY;
    }
    
    public static boolean getDefaultNetworkEnabled() {
        return DEFAULT_NETWORK_ENABLED;
    }
    
    public static String getDefaultNetworkEndpoint() {
        return DEFAULT_NETWORK_ENDPOINT;
    }
    
    public static boolean getDefaultCollectFps() {
        return DEFAULT_COLLECT_FPS;
    }
    
    public static boolean getDefaultCollectTps() {
        return DEFAULT_COLLECT_TPS;
    }
    
    public static boolean getDefaultCollectMspt() {
        return DEFAULT_COLLECT_MSPT;
    }
    
    public static boolean getDefaultCollectHeap() {
        return DEFAULT_COLLECT_HEAP;
    }
    
    public static boolean getDefaultCollectCpu() {
        return DEFAULT_COLLECT_CPU;
    }
    
    public static boolean getDefaultBinaryUnits() {
        return DEFAULT_BINARY_UNITS;
    }
}
