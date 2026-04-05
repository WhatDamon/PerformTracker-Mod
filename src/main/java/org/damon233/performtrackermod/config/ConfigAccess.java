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

package org.damon233.performtrackermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class ConfigAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String CONFIG_FILE_NAME = "performtracker.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int DEFAULT_OUTPUT_INTERVAL_SECONDS = 5;
    private static final boolean DEFAULT_CHAT_ENABLED = true;
    private static final boolean DEFAULT_EXPORT_ENABLED = true;
    private static final String DEFAULT_EXPORT_DIRECTORY = "performance_data";
    private static final String DEFAULT_OUTPUT_FORMAT = "csv";
    private static final boolean DEFAULT_COLLECT_FPS = true;
    private static final boolean DEFAULT_COLLECT_TPS = true;
    private static final boolean DEFAULT_COLLECT_MSPT = true;
    private static final boolean DEFAULT_COLLECT_HEAP = true;
    private static final boolean DEFAULT_COLLECT_CPU = true;
    private static final boolean DEFAULT_BINARY_UNITS = true;
    private static final boolean DEFAULT_NETWORK_ENABLED = false;
    private static final String DEFAULT_NETWORK_URL = "localhost";
    private static final int DEFAULT_RECEIVER_PORT = 31415;
    private static final int DEFAULT_SENDER_PORT = 31416;
    private static final Pattern RECEIVER_HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*$");

    private static ConfigData configData;
    private static boolean initialized = false;
    private static Path configFilePath;

    private static class ConfigData {
        int outputIntervalSeconds = DEFAULT_OUTPUT_INTERVAL_SECONDS;
        boolean chatEnabled = DEFAULT_CHAT_ENABLED;
        boolean exportEnabled = DEFAULT_EXPORT_ENABLED;
        String exportDirectory = DEFAULT_EXPORT_DIRECTORY;
        String outputFormat = DEFAULT_OUTPUT_FORMAT;
        boolean collectFps = DEFAULT_COLLECT_FPS;
        boolean collectTps = DEFAULT_COLLECT_TPS;
        boolean collectMspt = DEFAULT_COLLECT_MSPT;
        boolean collectHeap = DEFAULT_COLLECT_HEAP;
        boolean collectCpu = DEFAULT_COLLECT_CPU;
        boolean binaryUnits = DEFAULT_BINARY_UNITS;
        boolean networkEnabled = DEFAULT_NETWORK_ENABLED;
        String networkHost = DEFAULT_NETWORK_URL;
        int receiverPort = DEFAULT_RECEIVER_PORT;
        int senderPort = DEFAULT_SENDER_PORT;
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
                    if (!isValidOutputFormat(configData.outputFormat)) {
                        LOGGER.warn("Invalid outputFormat '{}' in config, falling back to csv", configData.outputFormat);
                        configData.outputFormat = DEFAULT_OUTPUT_FORMAT;
                        save();
                    }
                    LOGGER.info("Loaded config from {}", configFilePath);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read or parse config file, falling back to defaults", e);
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

    public static boolean isExportEnabled() {
        return configData != null ? configData.exportEnabled : DEFAULT_EXPORT_ENABLED;
    }

    public static String getExportDirectory() {
        return configData != null ? configData.exportDirectory : DEFAULT_EXPORT_DIRECTORY;
    }

    public static String getOutputFormat() {
        return configData != null ? configData.outputFormat : DEFAULT_OUTPUT_FORMAT;
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
            configData.outputIntervalSeconds = Math.clamp(seconds, 1, 3600);
            save();
        }
    }

    public static void setChatEnabled(boolean enabled) {
        if (configData != null) {
            configData.chatEnabled = enabled;
            save();
        }
    }

    public static void setExportEnabled(boolean enabled) {
        if (configData != null) {
            configData.exportEnabled = enabled;
            save();
        }
    }

    public static void setExportDirectory(String directory) {
        if (configData != null) {
            configData.exportDirectory = (directory != null && !directory.isBlank()) ? directory : DEFAULT_EXPORT_DIRECTORY;
            save();
        }
    }

    public static void setOutputFormat(String format) {
        if (configData != null) {
            String normalized = normalizeOutputFormat(format);
            configData.outputFormat = normalized != null ? normalized : DEFAULT_OUTPUT_FORMAT;
            save();
        }
    }

    private static String normalizeOutputFormat(String format) {
        if (format == null) {
            return null;
        }
        String lower = format.toLowerCase().trim();
        if (lower.equals("csv") || lower.equals("json") || lower.equals("yaml")) {
            return lower;
        }
        return null;
    }

    public static boolean isValidOutputFormat(String format) {
        return normalizeOutputFormat(format) != null;
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

    public static boolean isNetworkEnabled() {
        return configData != null ? configData.networkEnabled : DEFAULT_NETWORK_ENABLED;
    }

    public static void setNetworkEnabled(boolean enabled) {
        if (configData != null) {
            configData.networkEnabled = enabled;
            save();
            if (enabled) {
                org.damon233.performtrackermod.network.HttpServerManager.getInstance()
                        .start(configData.networkHost, configData.receiverPort);
            } else {
                org.damon233.performtrackermod.network.HttpServerManager.getInstance().stop();
            }
        }
    }

    public static String getNetworkUrl() {
        return configData != null ? configData.networkHost : DEFAULT_NETWORK_URL;
    }

    public static void setNetworkUrl(String url) {
        if (configData != null) {
            configData.networkHost = (url != null && !url.isBlank()) ? url : DEFAULT_NETWORK_URL;
            save();
        }
    }

    public static int getReceiverPort() {
        return configData != null ? configData.receiverPort : DEFAULT_RECEIVER_PORT;
    }

    public static void setReceiverPort(int port) {
        if (configData != null) {
            int clampedPort = Math.clamp(port, 1, 65535);
            if (configData.receiverPort != clampedPort) {
                configData.receiverPort = clampedPort;
                save();
                if (configData.networkEnabled) {
                    org.damon233.performtrackermod.network.HttpServerManager.getInstance()
                            .restart(configData.networkHost, clampedPort);
                }
            }
        }
    }

    public static int getSenderPort() {
        return configData != null ? configData.senderPort : DEFAULT_SENDER_PORT;
    }

    public static void setSenderPort(int port) {
        if (configData != null) {
            int clampedPort = Math.clamp(port, 1, 65535);
            if (configData.senderPort != clampedPort) {
                configData.senderPort = clampedPort;
                save();
            }
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

    public static boolean getDefaultExportEnabled() {
        return DEFAULT_EXPORT_ENABLED;
    }

    public static String getDefaultExportDirectory() {
        return DEFAULT_EXPORT_DIRECTORY;
    }

    public static String getDefaultOutputFormat() {
        return DEFAULT_OUTPUT_FORMAT;
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

    public static boolean getDefaultNetworkEnabled() {
        return DEFAULT_NETWORK_ENABLED;
    }

    public static String getDefaultNetworkUrl() {
        return DEFAULT_NETWORK_URL;
    }

    public static int getDefaultReceiverPort() {
        return DEFAULT_RECEIVER_PORT;
    }

    public static int getDefaultSenderPort() {
        return DEFAULT_SENDER_PORT;
    }

    public static boolean isValidNetworkUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return RECEIVER_HOST_PATTERN.matcher(url.trim()).matches();
    }
}
