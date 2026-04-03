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
            if (!first) sb.append(',');
            sb.append("\"tps\":").append(tps);
            first = false;
        }
        if (collectMspt) {
            if (!first) sb.append(',');
            sb.append("\"mspt\":").append(mspt);
            first = false;
        }
        if (collectHeap) {
            if (!first) sb.append(',');
            sb.append("\"heapUsed\":").append(heapUsed);
            sb.append(",\"heapMax\":").append(heapMax);
            first = false;
        }
        if (collectCpu) {
            if (!first) sb.append(',');
            sb.append("\"cpu\":").append(cpuUsage);
        }
        sb.append("}}");
        
        return sb.toString();
    }
    
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
