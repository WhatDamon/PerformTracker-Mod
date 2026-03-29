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
import net.minecraft.text.Text;

import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.collector.ServerMetricsCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.PerformanceMetrics;
import org.damon233.performtrackermod.network.HttpService;
import org.damon233.performtrackermod.network.JsonFormatter;
import org.damon233.performtrackermod.utils.CsvWriter;
import org.damon233.performtrackermod.utils.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackerController {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String CSV_BASENAME = "performance";

    private final ServerMetricsCollector serverCollector;
    private IFpsProvider fpsProvider;
    private final AtomicReference<TrackerState> state;
    private final AtomicBoolean active;

    private CsvWriter csvWriter;
    private String sessionId;
    private long lastOutputTime;
    private int sampleCount;
    private MinecraftServer server;
    private int lastCollectConfig;

    public TrackerController(ServerMetricsCollector serverCollector, IFpsProvider fpsProvider) {
        this.serverCollector = serverCollector;
        this.fpsProvider = fpsProvider;
        this.state = new AtomicReference<>(TrackerState.IDLE);
        this.active = new AtomicBoolean(false);
        this.lastOutputTime = 0;
        this.sampleCount = 0;
        this.csvWriter = null;
        this.sessionId = null;
        this.server = null;
        this.lastCollectConfig = 0;

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

        if (ConfigAccess.isCsvEnabled()) {
            try {
                csvWriter = new CsvWriter(ConfigAccess.getCsvDirectory(), CSV_BASENAME);
                csvWriter.writeHeader(buildCsvHeaders());
                csvWriter.start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create CSV file", e);
            }
        }

        if (ConfigAccess.isNetworkEnabled()) {
            HttpService httpService = PerformTracker.getHttpService();
            if (httpService != null && !httpService.tryStartServer()) {
                server.getPlayerManager().getPlayerList().forEach(player ->
                    player.sendMessage(TranslationService.chatError("performtracker.error.http_server_failed", ConfigAccess.getLocalServerPort()))
                );
            }
            httpService.start();
        }

        state.set(TrackerState.RUNNING);
        active.set(true);
        lastCollectConfig = getCollectConfigHash();

        serverCollector.reset();

        LOGGER.info("Performance tracking started, sessionId: {}", sessionId);
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

        if (csvWriter != null) {
            try {
                csvWriter.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close CSV file", e);
            }
            csvWriter = null;
        }

        HttpService httpService = PerformTracker.getHttpService();
        if (httpService != null) {
            httpService.stop();
        }

        LOGGER.info("Performance tracking stopped, samples: {}", sampleCount);
        
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
    
    private int getCollectConfigHash() {
        return (ConfigAccess.isCollectFps() ? 1 : 0) |
               (ConfigAccess.isCollectTps() ? 2 : 0) |
               (ConfigAccess.isCollectMspt() ? 4 : 0) |
               (ConfigAccess.isCollectHeap() ? 8 : 0) |
               (ConfigAccess.isCollectCpu() ? 16 : 0);
    }
    
    private String[] buildCsvHeaders() {
        StringBuilder sb = new StringBuilder();
        if (ConfigAccess.isCollectFps()) sb.append("fps,");
        if (ConfigAccess.isCollectTps()) sb.append("tps,");
        if (ConfigAccess.isCollectMspt()) sb.append("mspt,");
        if (ConfigAccess.isCollectHeap()) sb.append("heap_used,heap_max,");
        if (ConfigAccess.isCollectCpu()) sb.append("cpu,");
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString().split(",");
    }
    
    private void restartOutputs() {
        if (csvWriter != null) {
            try {
                csvWriter.close();
            } catch (IOException ignored) {
            }
            csvWriter = null;
        }
        
        boolean csvEnabled = ConfigAccess.isCsvEnabled();
        if (csvEnabled) {
            try {
                csvWriter = new CsvWriter(ConfigAccess.getCsvDirectory(), CSV_BASENAME);
                csvWriter.writeHeader(buildCsvHeaders());
                csvWriter.start();
            } catch (IOException e) {
                LOGGER.error("Failed to restart CSV writer", e);
            }
        }
        
        sampleCount = 0;
        
        if (ConfigAccess.isChatEnabled() && server != null) {
            String key = csvEnabled ? "restart.csv" : "restart";
            Object[] args = csvEnabled ? new Object[]{csvWriter.getFilePath().toString()} : new Object[]{};
            server.getPlayerManager().getPlayerList().forEach(player -> 
                player.sendMessage(TranslationService.chat(key, args))
            );
        }
    }

    private void onServerTick(MinecraftServer server) {
        this.server = server;

        if (!active.get()) {
            return;
        }

        int currentConfig = getCollectConfigHash();
        if (currentConfig != lastCollectConfig) {
            LOGGER.info("Collect config changed, restarting tracking");
            lastCollectConfig = currentConfig;
            restartOutputs();
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
            String chatMsg = buildChatMessage(metrics);
            server.getPlayerManager().getPlayerList().forEach(player -> 
                player.sendMessage(TranslationService.chatWithMetrics(chatMsg))
            );
        }

        if (csvWriter != null) {
            csvWriter.enqueue(buildCsvRowValues(metrics));
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
    
    private String buildChatMessage(PerformanceMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (ConfigAccess.isCollectFps()) {
            if (!first) sb.append(" | ");
            sb.append(TranslationService.colorLabel("FPS: ")).append(TranslationService.colorValue(String.format("%.1f", metrics.fps())));
            first = false;
        }
        if (ConfigAccess.isCollectTps()) {
            if (!first) sb.append(" | ");
            sb.append(TranslationService.colorLabel("TPS: ")).append(TranslationService.colorValue(PerformanceMetrics.formatValue(metrics.tps())));
            first = false;
        }
        if (ConfigAccess.isCollectMspt()) {
            if (!first) sb.append(" | ");
            sb.append(TranslationService.colorLabel("MSPT: ")).append(TranslationService.colorValue(PerformanceMetrics.formatValue(metrics.mspt())));
            first = false;
        }
        if (ConfigAccess.isCollectHeap()) {
            if (!first) sb.append(" | ");
            sb.append(TranslationService.colorLabel("Heap: "))
              .append(TranslationService.colorValue(PerformanceMetrics.formatMemoryMB(metrics.heapUsed())))
              .append(" / ")
              .append(TranslationService.colorValue(PerformanceMetrics.formatMemoryMB(metrics.heapMax())));
        }
        if (ConfigAccess.isCollectCpu()) {
            if (!first) sb.append(" | ");
            sb.append(TranslationService.colorLabel("CPU: "))
              .append(TranslationService.colorValue(String.format("%.1f%%", metrics.cpuUsage())));
        }
        return sb.toString();
    }
    
    private Object[] buildCsvRowValues(PerformanceMetrics metrics) {
        int count = 0;
        if (ConfigAccess.isCollectFps()) count++;
        if (ConfigAccess.isCollectTps()) count++;
        if (ConfigAccess.isCollectMspt()) count++;
        if (ConfigAccess.isCollectHeap()) count += 2;
        if (ConfigAccess.isCollectCpu()) count++;
        
        Object[] values = new Object[count];
        int i = 0;
        if (ConfigAccess.isCollectFps()) values[i++] = metrics.fps();
        if (ConfigAccess.isCollectTps()) values[i++] = metrics.tps();
        if (ConfigAccess.isCollectMspt()) values[i++] = metrics.mspt();
        if (ConfigAccess.isCollectHeap()) {
            values[i++] = metrics.heapUsed();
            values[i++] = metrics.heapMax();
        }
        if (ConfigAccess.isCollectCpu()) values[i++] = metrics.cpuUsage();
        return values;
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

    public String getCsvFilePath() {
        if (csvWriter != null) {
            return csvWriter.getFilePath().toString();
        }
        return "N/A";
    }

    public int getSampleCount() {
        return sampleCount;
    }
    
    public boolean isNetworkEnabled() {
        return ConfigAccess.isNetworkEnabled();
    }
}
