package org.damon233.performtrackermod.controller;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import org.damon233.performtrackermod.collector.ServerTickCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.PerformanceMetrics;
import org.damon233.performtrackermod.utils.CsvFileWriter;
import org.damon233.performtrackermod.utils.TranslationService;

public class TrackerController {
    private static final String CSV_BASENAME = "performance";
    private static final String CSV_STATUS_FINAL = "FINAL";

    private final ServerTickCollector serverCollector;
    private IFpsProvider fpsProvider;
    private final AtomicReference<TrackerState> state;
    private final AtomicBoolean active;

    private CsvFileWriter csvWriter;
    private int tickCounter;
    private int sampleCount;
    private MinecraftServer server;

    private static TrackerController instance;

    public TrackerController(ServerTickCollector serverCollector, IFpsProvider fpsProvider) {
        this.serverCollector = serverCollector;
        this.fpsProvider = fpsProvider;
        this.state = new AtomicReference<>(TrackerState.IDLE);
        this.active = new AtomicBoolean(false);
        this.tickCounter = 0;
        this.sampleCount = 0;
        this.csvWriter = null;
        this.server = null;
        instance = this;

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public static TrackerController getInstance() {
        return instance;
    }

    public synchronized void start() {
        if (state.get() == TrackerState.RUNNING) {
            throw new IllegalStateException("error.already_running");
        }

        if (ConfigAccess.isCsvEnabled()) {
            try {
                csvWriter = new CsvFileWriter(ConfigAccess.getCsvDirectory(), CSV_BASENAME);
                csvWriter.writeHeader("fps", "tps", "mspt", "status");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create CSV file", e);
            }
        }

        state.set(TrackerState.RUNNING);
        active.set(true);
    }

    public synchronized void stop() {
        if (state.get() == TrackerState.IDLE) {
            throw new IllegalStateException("error.not_running");
        }

        active.set(false);

        if (csvWriter != null) {
            try {
                PerformanceMetrics metrics = getMetrics();
                csvWriter.writeRow(metrics.getFps(), metrics.getTps(), metrics.getMspt(), CSV_STATUS_FINAL);
                csvWriter.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close CSV file", e);
            }
            csvWriter = null;
        }

        state.set(TrackerState.IDLE);
        tickCounter = 0;
        sampleCount = 0;

        serverCollector.reset();
        if (fpsProvider != null) {
            fpsProvider.reset();
        }
    }

    private void onServerTick(MinecraftServer server) {
        this.server = server;

        if (!active.get()) {
            return;
        }

        tickCounter++;

        if (tickCounter >= ConfigAccess.getOutputIntervalTicks()) {
            tickCounter = 0;
            outputMetrics();
        }
    }

    private void outputMetrics() {
        PerformanceMetrics metrics = getMetrics();
        sampleCount++;

        if (ConfigAccess.isChatEnabled() && server != null) {
            server.getPlayerManager().getPlayerList().forEach(player -> 
                player.sendMessage(TranslationService.chatWithMetrics(metrics.toChatString()))
            );
        }

        if (csvWriter != null) {
            try {
                csvWriter.writeRow(metrics.getFps(), metrics.getTps(), metrics.getMspt(), metrics.getStatusIndicator());
            } catch (IOException e) {
                // Silently fail - don't disrupt tracking
            }
        }
    }

    public TrackerState getState() {
        return state.get();
    }

    public int getOutputIntervalTicks() {
        return ConfigAccess.getOutputIntervalTicks();
    }

    public PerformanceMetrics getMetrics() {
        double fps = fpsProvider.getAverageFps();
        double tps = serverCollector.getTps();
        double mspt = serverCollector.getMspt();
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
