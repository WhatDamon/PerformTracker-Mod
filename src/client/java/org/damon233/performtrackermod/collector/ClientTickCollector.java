package org.damon233.performtrackermod.collector;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import org.damon233.performtrackermod.utils.ThreadSafeCounter;

public class ClientTickCollector implements IFpsProvider {
    
    private static final int DEFAULT_WINDOW_SIZE = 60;
    
    private final int[] fpsWindow;
    private int windowIndex;
    private int sampleCount;
    private final ThreadSafeCounter fpsCounter;
    private volatile boolean enabled;
    
    public ClientTickCollector() {
        this(DEFAULT_WINDOW_SIZE);
    }
    
    public ClientTickCollector(int windowSize) {
        this.fpsWindow = new int[Math.max(1, windowSize)];
        this.windowIndex = 0;
        this.sampleCount = 0;
        this.fpsCounter = new ThreadSafeCounter();
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
        
        fpsCounter.add(currentFps);
    }
    
    public int getFps() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 0;
        }
        return client.getCurrentFps();
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
    
    public double getCounterAverageFps() {
        return fpsCounter.getAverage();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void reset() {
        windowIndex = 0;
        sampleCount = 0;
        fpsCounter.reset();
        
        for (int i = 0; i < fpsWindow.length; i++) {
            fpsWindow[i] = 0;
        }
    }
    
    public int getSampleCount() {
        return sampleCount;
    }
    
    public int getWindowSize() {
        return fpsWindow.length;
    }
    
    public void shutdown() {
        enabled = false;
        reset();
    }
}
