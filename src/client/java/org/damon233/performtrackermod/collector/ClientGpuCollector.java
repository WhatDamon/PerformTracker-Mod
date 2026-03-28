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
