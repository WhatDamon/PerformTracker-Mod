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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GpuDetector {
    private static final String VULKAN_DEVICE_EXTENSIONS = "VK_KHR_device_group";

    public static String getGpuName() {
        String vulkanGpu = tryVulkanDetection();
        if (vulkanGpu != null) {
            return vulkanGpu;
        }

        return tryOpenGLDetection();
    }

    private static String tryVulkanDetection() {
        try {
            Class<?> vkInstanceClass = Class.forName("org.lwjgl.vulkan.VkInstance");
            Method getPhysicalDevices = vkInstanceClass.getMethod("getPhysicalDevices");
            Object vkInstance = createVulkanInstance();
            if (vkInstance == null) {
                return null;
            }
            Object physicalDevices = getPhysicalDevices.invoke(vkInstance);
            if (physicalDevices != null) {
                return extractVulkanGpuName(physicalDevices);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                 IllegalAccessException | InvocationTargetException e) {
        }
        return null;
    }

    private static Object createVulkanInstance() {
        try {
            Class<?> VkInstance = Class.forName("org.lwjgl.vulkan.VkInstance");
            Class<?> VkInstanceCreateInfo = Class.forName("org.lwjgl.vulkan.VkInstanceCreateInfo");
            Method create = VkInstance.getMethod("create", VkInstanceCreateInfo, java.nio.ByteBuffer.class);
            Object createInfo = VkInstanceCreateInfo.getDeclaredConstructor().newInstance();
            return create.invoke(null, createInfo, (java.nio.ByteBuffer) null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractVulkanGpuName(Object physicalDevices) {
        try {
            if (physicalDevices instanceof java.util.List) {
                java.util.List<?> devices = (java.util.List<?>) physicalDevices;
                if (!devices.isEmpty()) {
                    Object device = devices.get(0);
                    Class<?> VkPhysicalDevice = Class.forName("org.lwjgl.vulkan.VkPhysicalDevice");
                    Method getProperties = VkPhysicalDevice.getMethod("getProperties");
                    Object properties = getProperties.invoke(device);
                    return getDeviceName(properties);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getDeviceName(Object properties) {
        try {
            Method getDeviceName = properties.getClass().getMethod("deviceName");
            return (String) getDeviceName.invoke(properties);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String tryOpenGLDetection() {
        try {
            String renderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
            String vendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);

            if (renderer != null && !renderer.isEmpty()) {
                return renderer;
            }
            if (vendor != null && !vendor.isEmpty()) {
                return vendor;
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }
}
