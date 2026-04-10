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

package org.damon233.performtrackermod.network;

import org.damon233.performtrackermod.data.SystemInfo;

public class JsonFormatter {
    
    public static String formatMetrics(long timestamp, String sessionId,
                                       int sampleNumber,
                                       boolean collectFps, double fps,
                                       boolean collectTps, double tps,
                                       boolean collectMspt, double mspt,
                                       boolean collectHeap, double heapUsed, double heapMax,
                                       boolean collectCpu, double cpuUsage) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"timestamp\":").append(timestamp);
        sb.append(",\"sessionId\":\"").append(sessionId).append('"');
        sb.append(",\"sampleNumber\":").append(sampleNumber);
        sb.append(",\"data\":{");
        
        boolean first = true;
        if (collectFps) {
            sb.append("\"fps\":").append(fps);
            first = false;
        }
        if (collectTps) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"tps\":").append(tps);
            first = false;
        }
        if (collectMspt) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"mspt\":").append(mspt);
            first = false;
        }
        if (collectHeap) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"heapUsed\":").append(heapUsed);
            sb.append(",\"heapMax\":").append(heapMax);
            first = false;
        }
        if (collectCpu) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"cpu\":").append(cpuUsage);
        }
        sb.append("}}");
        
        return sb.toString();
    }

    public static String formatDeviceInfo(SystemInfo info, boolean chipRulesTrusted) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"deviceType\":\"").append(escapeJson(info.deviceType().getCode())).append('"');
        sb.append(",\"deviceModel\":\"").append(escapeJson(info.deviceModel() != null ? info.deviceModel() : "Unknown")).append('"');
        sb.append(",\"cpuName\":\"").append(escapeJson(info.cpuName() != null ? info.cpuName() : "Unknown")).append('"');
        sb.append(",\"gpuName\":\"").append(escapeJson(info.gpuName() != null ? info.gpuName() : "Unknown")).append('"');
        sb.append(",\"cpuCores\":").append(info.cpuCores());
        sb.append(",\"memory\":").append(info.totalMemoryBytes());
        sb.append(",\"os\":\"").append(escapeJson(info.osName())).append('"');
        sb.append(",\"osVersion\":\"").append(escapeJson(info.osVersion())).append('"');
        sb.append(",\"osArch\":\"").append(escapeJson(info.osArch())).append('"');
        sb.append(",\"javaVersion\":\"").append(escapeJson(info.javaVersion())).append('"');
        sb.append(",\"jvmName\":\"").append(escapeJson(info.jvmName())).append('"');
        sb.append(",\"minecraftVersion\":\"").append(escapeJson(info.minecraftVersion())).append('"');
        sb.append(",\"modVersion\":\"").append(escapeJson(info.modVersion())).append('"');
        sb.append(",\"jvmArgs\":[");
        String[] jvmArgs = info.jvmArgs();
        if (jvmArgs != null) {
            for (int i = 0; i < jvmArgs.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(escapeJson(jvmArgs[i])).append('"');
            }
        }
        sb.append("],\"chipRulesTrusted\":").append(chipRulesTrusted).append('}');
        return sb.toString();
    }

    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
