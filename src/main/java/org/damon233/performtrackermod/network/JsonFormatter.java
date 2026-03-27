package org.damon233.performtrackermod.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.damon233.performtrackermod.data.PerformanceMetrics;

public class JsonFormatter {
    private static final Gson GSON = new GsonBuilder().create();
    
    public static String formatMetrics(long timestamp, String sessionId, 
                                       boolean sessionActive, int sampleNumber,
                                       PerformanceMetrics metrics,
                                       boolean collectFps, boolean collectTps, boolean collectMspt) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", "performance_metrics");
        payload.put("timestamp", timestamp);
        payload.put("unixTimestamp", timestamp / 1000);
        payload.put("sessionId", sessionId);
        payload.put("sessionActive", sessionActive);
        payload.put("sampleNumber", sampleNumber);
        
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        if (collectFps) data.put("fps", metrics.fps());
        if (collectTps) data.put("tps", metrics.tps());
        if (collectMspt) data.put("mspt", metrics.mspt());
        payload.put("data", data);
        
        return GSON.toJson(payload);
    }
    
    public static String formatMetrics(long timestamp, String sessionId, 
                                       boolean sessionActive, int sampleNumber,
                                       PerformanceMetrics metrics) {
        return formatMetrics(timestamp, sessionId, sessionActive, sampleNumber, metrics, true, true, true);
    }
    
    public static String formatMessage(String message) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", "message");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("unixTimestamp", System.currentTimeMillis() / 1000);
        payload.put("message", message);
        return GSON.toJson(payload);
    }
}
