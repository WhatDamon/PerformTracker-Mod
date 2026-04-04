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

package org.damon233.performtrackermod.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.collector.ServerMetricsCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.PerformanceMetrics;
import org.damon233.performtrackermod.network.HttpService;
import org.damon233.performtrackermod.network.JsonFormatter;
import org.damon233.performtrackermod.utils.FormattingService;
import org.damon233.performtrackermod.writer.CsvWriter;
import org.damon233.performtrackermod.writer.JsonWriter;
import org.damon233.performtrackermod.writer.MetricsWriter;
import org.damon233.performtrackermod.writer.YamlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackerController {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String EXPORT_BASENAME = "performance";

    private final ServerMetricsCollector serverCollector;
    private IFpsProvider fpsProvider;
    private final AtomicReference<TrackerState> state;
    private final AtomicBoolean active;

    private MetricsWriter metricsWriter;
    private String sessionId;
    private long lastOutputTime;
    private int sampleCount;
    private MinecraftServer server;
    private String currentOutputFormat;

    private final Object[] rowValuesBuffer = new Object[6];
    private final MutableText[] chatMessageParts = new MutableText[10];
    private int chatMessagePartCount;

    public TrackerController(ServerMetricsCollector serverCollector, IFpsProvider fpsProvider) {
        this.serverCollector = serverCollector;
        this.fpsProvider = fpsProvider;
        this.state = new AtomicReference<>(TrackerState.IDLE);
        this.active = new AtomicBoolean(false);
        this.lastOutputTime = 0;
        this.sampleCount = 0;
        this.metricsWriter = null;
        this.sessionId = null;
        this.server = null;

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }
    
    private void onServerStopping(MinecraftServer server) {
        if (isRunning()) {
            LOGGER.info("Server stopping, auto-stopping performance tracker");
            try {
                stop();
            } catch (Exception e) {
                LOGGER.error("Failed to stop tracker on server shutdown", e);
            }
        }
    }

    public synchronized void start(MinecraftServer server) {
        if (state.get() == TrackerState.RUNNING) {
            throw new IllegalStateException("error.already_running");
        }

        if (!hasAnyMetricEnabled()) {
            throw new IllegalStateException("error.no_metrics_enabled");
        }

        this.sessionId = generateSessionId();
        this.server = server;

        if (ConfigAccess.isExportEnabled()) {
            try {
                metricsWriter = createMetricsWriter();
                metricsWriter.writeHeader(buildHeaders());
                metricsWriter.start();
                currentOutputFormat = ConfigAccess.getOutputFormat();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output file", e);
            }
        }

        if (ConfigAccess.isNetworkEnabled()) {
            HttpService httpService = PerformTracker.getHttpService();
            if (httpService != null) {
                if (!httpService.tryStartServer()) {
                    server.getPlayerManager().getPlayerList().forEach(player ->
                        player.sendMessage(FormattingService.chatError("performtracker.error.http_server_failed", ConfigAccess.getLocalServerPort()))
                    );
                }
                httpService.start();
            }
        }

        state.set(TrackerState.RUNNING);
        active.set(true);

        serverCollector.reset();

        LOGGER.debug("Performance tracking started, sessionId: {}", sessionId);
    }

    private boolean hasAnyMetricEnabled() {
        return ConfigAccess.isCollectFps() ||
                ConfigAccess.isCollectTps() ||
                ConfigAccess.isCollectMspt() ||
                ConfigAccess.isCollectHeap() ||
                ConfigAccess.isCollectCpu();
    }

    public synchronized void stop() {
        if (state.get() == TrackerState.IDLE) {
            throw new IllegalStateException("error.not_running");
        }

        active.set(false);

        if (metricsWriter != null) {
            metricsWriter.close();
            metricsWriter = null;
        }

        HttpService httpService = PerformTracker.getHttpService();
        if (httpService != null) {
            httpService.stop();
        }

        LOGGER.debug("Performance tracking stopped, samples: {}", sampleCount);
        
        state.set(TrackerState.IDLE);
        lastOutputTime = 0;
        sampleCount = 0;
        sessionId = null;

        serverCollector.reset();
        if (fpsProvider != null) {
            fpsProvider.reset();
        }
    }
    
    private String generateSessionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6);
        return timestamp + "_" + uuid;
    }

    private String[] buildHeaders() {
        return new String[]{"fps", "tps", "mspt", "heap_used", "heap_max", "cpu"};
    }

    private void onServerTick(MinecraftServer server) {
        this.server = server;

        if (!active.get()) {
            return;
        }

        if (ConfigAccess.isExportEnabled() && metricsWriter != null) {
            String newFormat = ConfigAccess.getOutputFormat();
            if (!newFormat.equals(currentOutputFormat)) {
                LOGGER.debug("Output format changed from {} to {}, creating new file", currentOutputFormat, newFormat);
                try {
                    metricsWriter.close();
                    metricsWriter = createMetricsWriter();
                    metricsWriter.writeHeader(buildHeaders());
                    metricsWriter.start();
                    currentOutputFormat = newFormat;
                } catch (IOException e) {
                    LOGGER.error("Failed to create new output file after format change", e);
                }
            }
        }

        long currentTime = System.currentTimeMillis();
        long intervalMs = ConfigAccess.getOutputIntervalSeconds() * 1000L;

        if (lastOutputTime == 0 || (currentTime - lastOutputTime) >= intervalMs) {
            lastOutputTime = currentTime;
            outputMetrics();
        }
    }

    private void outputMetrics() {
        PerformanceMetrics metrics = getMetrics();
        sampleCount++;
        long timestamp = System.currentTimeMillis();

        if (ConfigAccess.isChatEnabled() && server != null) {
            MutableText chatMsg = FormattingService.chatWithMetrics(buildChatMessage(metrics));
            server.getPlayerManager().getPlayerList().forEach(player -> 
                player.sendMessage(chatMsg)
            );
        }

        if (metricsWriter != null) {
            metricsWriter.enqueue(buildRowValues(metrics));
        }

        if (ConfigAccess.isNetworkEnabled() && sessionId != null) {
            HttpService httpService = PerformTracker.getHttpService();
            if (httpService != null) {
                String json = JsonFormatter.formatMetrics(timestamp, sessionId, sampleCount,
                    ConfigAccess.isCollectFps(), metrics.fps(),
                    ConfigAccess.isCollectTps(), metrics.tps(),
                    ConfigAccess.isCollectMspt(), metrics.mspt(),
                    ConfigAccess.isCollectHeap(), metrics.heapUsed(), metrics.heapMax(),
                    ConfigAccess.isCollectCpu(), metrics.cpuUsage());
                httpService.send(json);
            }
        }
    }
    
    private MutableText buildChatMessage(PerformanceMetrics metrics) {
        chatMessagePartCount = 0;
        if (ConfigAccess.isCollectFps()) {
            addChatPart("FPS: ", String.format("%.1f", metrics.fps()));
        }
        if (ConfigAccess.isCollectTps()) {
            addChatPart("TPS: ", PerformanceMetrics.formatValue(metrics.tps()));
        }
        if (ConfigAccess.isCollectMspt()) {
            addChatPart("MSPT: ", PerformanceMetrics.formatValue(metrics.mspt()));
        }
        if (ConfigAccess.isCollectHeap()) {
            addChatPart("Heap: ", PerformanceMetrics.formatMemoryMB(metrics.heapUsed()) + " / " + PerformanceMetrics.formatMemoryMB(metrics.heapMax()));
        }
        if (ConfigAccess.isCollectCpu()) {
            addChatPart("CPU: ", String.format("%.1f%%", metrics.cpuUsage()));
        }

        MutableText result = Text.empty();
        for (int i = 0; i < chatMessagePartCount; i++) {
            if (i > 0) {
                result.append(Text.literal(" | ").withColor(0x888888));
            }
            result.append(chatMessageParts[i]);
        }
        return result;
    }

    private void addChatPart(String label, String value) {
        if (chatMessagePartCount >= chatMessageParts.length) {
            return;
        }
        chatMessageParts[chatMessagePartCount++] = Text.literal(label).withColor(0x888888).append(FormattingService.colorValue(value));
    }

    private Object[] buildRowValues(PerformanceMetrics metrics) {
        rowValuesBuffer[0] = ConfigAccess.isCollectFps() ? metrics.fps() : Double.NaN;
        rowValuesBuffer[1] = ConfigAccess.isCollectTps() ? metrics.tps() : Double.NaN;
        rowValuesBuffer[2] = ConfigAccess.isCollectMspt() ? metrics.mspt() : Double.NaN;
        rowValuesBuffer[3] = ConfigAccess.isCollectHeap() ? metrics.heapUsed() : Double.NaN;
        rowValuesBuffer[4] = ConfigAccess.isCollectHeap() ? metrics.heapMax() : Double.NaN;
        rowValuesBuffer[5] = ConfigAccess.isCollectCpu() ? metrics.cpuUsage() : Double.NaN;
        return rowValuesBuffer;
    }

    public PerformanceMetrics getMetrics() {
        double fps = (fpsProvider != null && ConfigAccess.isCollectFps()) ? fpsProvider.getAverageFps() : 0;
        double tps = ConfigAccess.isCollectTps() ? serverCollector.getTps() : 0;
        double mspt = ConfigAccess.isCollectMspt() ? serverCollector.getMspt() : 0;
        double heapUsed = ConfigAccess.isCollectHeap() ? serverCollector.getHeapUsedMB() : 0;
        double heapMax = ConfigAccess.isCollectHeap() ? serverCollector.getHeapMaxMB() : 0;
        double cpuUsage = ConfigAccess.isCollectCpu() ? serverCollector.getCpuUsagePercent() : -1;
        return new PerformanceMetrics(fps, tps, mspt, heapUsed, heapMax, cpuUsage);
    }

    public boolean isRunning() {
        return state.get() == TrackerState.RUNNING;
    }

    public void setFpsProvider(IFpsProvider fpsProvider) {
        this.fpsProvider = fpsProvider;
    }

    public String getExportFilePath() {
        if (metricsWriter != null) {
            return metricsWriter.getFilePath().toString();
        }
        return "N/A";
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public boolean isNetworkEnabled() {
        return ConfigAccess.isNetworkEnabled();
    }

    private MetricsWriter createMetricsWriter() throws IOException {
        String format = ConfigAccess.getOutputFormat();
        String directory = ConfigAccess.getExportDirectory();
        return switch (format) {
            case "json" -> new JsonWriter(directory, EXPORT_BASENAME);
            case "yaml" -> new YamlWriter(directory, EXPORT_BASENAME);
            default -> new CsvWriter(directory, EXPORT_BASENAME);
        };
    }
}
