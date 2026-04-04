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

package org.damon233.performtrackermod.collector;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;

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

        Arrays.fill(fpsWindow, 0);
    }
}
