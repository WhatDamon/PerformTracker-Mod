package org.damon233.performtrackermod.collector;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ClientMetricsCollector implements IFpsProvider {
    
    private static final int DEFAULT_WINDOW_SIZE = 60;
    
    private final int[] fpsWindow;
    private int windowIndex;
    private int sampleCount;
    private volatile boolean enabled;
    
    public ClientMetricsCollector() {
        this(DEFAULT_WINDOW_SIZE);
    }
    
    public ClientMetricsCollector(int windowSize) {
        this.fpsWindow = new int[Math.max(1, windowSize)];
        this.windowIndex = 0;
        this.sampleCount = 0;
        this.enabled = true;
        
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!enabled || client == null) {
            return;
        }
        
        MinecraftClient instance = MinecraftClient.getInstance();
        if (instance == null) {
            return;
        }
        
        int currentFps = instance.getCurrentFps();
        
        fpsWindow[windowIndex] = currentFps;
        windowIndex = (windowIndex + 1) % fpsWindow.length;
        if (sampleCount < fpsWindow.length) {
            sampleCount++;
        }
    }
    
    public double getAverageFps() {
        if (sampleCount == 0) {
            return 0.0;
        }
        
        long sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += fpsWindow[i];
        }
        return (double) sum / sampleCount;
    }

    public void reset() {
        windowIndex = 0;
        sampleCount = 0;

        for (int i = 0; i < fpsWindow.length; i++) {
            fpsWindow[i] = 0;
        }
    }
}
