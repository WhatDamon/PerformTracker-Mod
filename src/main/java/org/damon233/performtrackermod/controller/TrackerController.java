package org.damon233.performtrackermod.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import org.damon233.performtrackermod.collector.ServerTickCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.PerformanceMetrics;
import org.damon233.performtrackermod.network.HttpSender;
import org.damon233.performtrackermod.network.JsonFormatter;
import org.damon233.performtrackermod.utils.CsvFileWriter;
import org.damon233.performtrackermod.utils.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackerController {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final String CSV_BASENAME = "performance";

    private final ServerTickCollector serverCollector;
    private IFpsProvider fpsProvider;
    private final AtomicReference<TrackerState> state;
    private final AtomicBoolean active;

    private CsvFileWriter csvWriter;
    private HttpSender httpSender;
    private String sessionId;
    private long lastOutputTime;
    private int sampleCount;
    private MinecraftServer server;
    private int lastCollectConfig;

    private static TrackerController instance;

    public TrackerController(ServerTickCollector serverCollector, IFpsProvider fpsProvider) {
        this.serverCollector = serverCollector;
        this.fpsProvider = fpsProvider;
        this.state = new AtomicReference<>(TrackerState.IDLE);
        this.active = new AtomicBoolean(false);
        this.lastOutputTime = 0;
        this.sampleCount = 0;
        this.csvWriter = null;
        this.httpSender = null;
        this.sessionId = null;
        this.server = null;
        this.lastCollectConfig = 0;
        instance = this;

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

    public static TrackerController getInstance() {
        return instance;
    }

    public synchronized void start() {
        if (state.get() == TrackerState.RUNNING) {
            throw new IllegalStateException("error.already_running");
        }

        this.sessionId = generateSessionId();

        if (ConfigAccess.isCsvEnabled()) {
            try {
                csvWriter = new CsvFileWriter(ConfigAccess.getCsvDirectory(), CSV_BASENAME);
                StringBuilder header = new StringBuilder();
                if (ConfigAccess.isCollectFps()) header.append("fps,");
                if (ConfigAccess.isCollectTps()) header.append("tps,");
                if (ConfigAccess.isCollectMspt()) header.append("mspt,");
                if (header.length() > 0) {
                    header.setLength(header.length() - 1);
                }
                csvWriter.writeHeader(header.toString().split(","));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create CSV file", e);
            }
        }

        if (ConfigAccess.isNetworkEnabled()) {
            httpSender = new HttpSender();
            httpSender.initialize(ConfigAccess.getNetworkUrl());
            httpSender.start();
        }

        state.set(TrackerState.RUNNING);
        active.set(true);
        lastCollectConfig = getCollectConfigHash();
        
        serverCollector.reset();
        
        LOGGER.info("Performance tracking started, sessionId: {}", sessionId);
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

        if (httpSender != null) {
            httpSender.stop();
            httpSender = null;
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
               (ConfigAccess.isCollectMspt() ? 4 : 0);
    }
    
    private void restartOutputs() {
        if (csvWriter != null) {
            try {
                csvWriter.close();
            } catch (IOException e) {
                // ignore
            }
            csvWriter = null;
        }
        
        boolean csvEnabled = ConfigAccess.isCsvEnabled();
        if (csvEnabled) {
            try {
                csvWriter = new CsvFileWriter(ConfigAccess.getCsvDirectory(), CSV_BASENAME);
                StringBuilder header = new StringBuilder();
                if (ConfigAccess.isCollectFps()) header.append("fps,");
                if (ConfigAccess.isCollectTps()) header.append("tps,");
                if (ConfigAccess.isCollectMspt()) header.append("mspt,");
                if (header.length() > 0) {
                    header.setLength(header.length() - 1);
                }
                csvWriter.writeHeader(header.toString().split(","));
            } catch (IOException e) {
                LOGGER.error("Failed to restart CSV writer", e);
            }
        }
        
        sampleCount = 0;
        
        if (ConfigAccess.isChatEnabled() && server != null) {
            net.minecraft.text.MutableText message = csvEnabled 
                ? TranslationService.chat("restart.csv", csvWriter.getFilePath().toString())
                : TranslationService.chat("restart");
            server.getPlayerManager().getPlayerList().forEach(player -> 
                player.sendMessage(message)
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
            try {
                writeCsvRow(metrics);
            } catch (IOException e) {
                // Silently fail - don't disrupt tracking
            }
        }

        if (httpSender != null && sessionId != null) {
            String json = JsonFormatter.formatMetrics(timestamp, sessionId, true, sampleCount, metrics,
                ConfigAccess.isCollectFps(), ConfigAccess.isCollectTps(), ConfigAccess.isCollectMspt());
            httpSender.send(json);
        }
    }
    
    private String buildChatMessage(PerformanceMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (ConfigAccess.isCollectFps()) {
            sb.append("FPS: ").append(String.format("%.1f", metrics.fps()));
            first = false;
        }
        if (ConfigAccess.isCollectTps()) {
            if (!first) sb.append(" | ");
            sb.append("TPS: ").append(formatValue(metrics.tps()));
            first = false;
        }
        if (ConfigAccess.isCollectMspt()) {
            if (!first) sb.append(" | ");
            sb.append("MSPT: ").append(formatValue(metrics.mspt()));
        }
        return sb.toString();
    }
    
    private String formatValue(double value) {
        if (Double.isInfinite(value)) {
            return "\u221E";
        }
        return String.format("%.2f", value);
    }
    
    private void writeCsvRow(PerformanceMetrics metrics) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (ConfigAccess.isCollectFps()) sb.append(metrics.fps()).append(",");
        if (ConfigAccess.isCollectTps()) sb.append(metrics.tps()).append(",");
        if (ConfigAccess.isCollectMspt()) sb.append(metrics.mspt()).append(",");
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        csvWriter.writeRow(sb.toString().split(","));
    }

    public TrackerState getState() {
        return state.get();
    }

    public PerformanceMetrics getMetrics() {
        double fps = ConfigAccess.isCollectFps() ? fpsProvider.getAverageFps() : 0;
        double tps = ConfigAccess.isCollectTps() ? serverCollector.getTps() : 0;
        double mspt = ConfigAccess.isCollectMspt() ? serverCollector.getMspt() : 0;
        return new PerformanceMetrics(fps, tps, mspt);
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
}
