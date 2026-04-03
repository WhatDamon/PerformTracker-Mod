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

public class ClientGpuCollector implements IGpuProvider {
    private String gpuName = null;
    private boolean initialized = false;

    public ClientGpuCollector() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (initialized || client == null) {
            return;
        }

        MinecraftClient instance = MinecraftClient.getInstance();
        if (instance == null) {
            return;
        }

        try {
            gpuName = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
            if (gpuName == null || gpuName.isEmpty()) {
                gpuName = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
            }
        } catch (Exception e) {
            gpuName = "Unknown";
        }

        if (gpuName == null || gpuName.isEmpty()) {
            gpuName = "Unknown";
        }

        initialized = true;
    }

    @Override
    public String getGpuName() {
        if (!initialized) {
            onClientTick(null);
        }
        return gpuName != null ? gpuName : "Unknown";
    }
}
