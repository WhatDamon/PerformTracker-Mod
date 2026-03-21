package org.damon233.performtrackermod.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.damon233.performtrackermod.data.PerformanceMetrics;

/**
 * Formats performance data as JSON for network transmission.
 */
public class JsonFormatter {
    private static final Gson GSON = new GsonBuilder().create();
    
    /**
     * Data payload containing performance metrics.
     */
    public static class MetricsPayload {
        public final String type = "performance_metrics";
        public final long timestamp;
        public final long unixTimestamp;
        public final String sessionId;
        public final boolean sessionActive;
        public final int sampleNumber;
        public final MetricsData data;
        
        public MetricsPayload(long timestamp, String sessionId, boolean sessionActive, 
                              int sampleNumber, PerformanceMetrics metrics) {
            this.timestamp = timestamp;
            this.unixTimestamp = timestamp / 1000;
            this.sessionId = sessionId;
            this.sessionActive = sessionActive;
            this.sampleNumber = sampleNumber;
            this.data = new MetricsData(metrics);
        }
    }
    
    /**
     * Metrics data fields.
     */
    public static class MetricsData {
        public final double fps;
        public final double tps;
        public final double mspt;
        public final String status;
        
        public MetricsData(PerformanceMetrics metrics) {
            this.fps = metrics.getFps();
            this.tps = metrics.getTps();
            this.mspt = metrics.getMspt();
            this.status = metrics.getStatusIndicator();
        }
    }
    
    /**
     * Format metrics as JSON string.
     */
    public static String formatMetrics(long timestamp, String sessionId, 
                                       boolean sessionActive, int sampleNumber,
                                       PerformanceMetrics metrics) {
        MetricsPayload payload = new MetricsPayload(timestamp, sessionId, 
                                                    sessionActive, sampleNumber, metrics);
        return GSON.toJson(payload);
    }
    
    /**
     * Format simple text as JSON with a message.
     */
    public static String formatMessage(String message) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", "message");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("unixTimestamp", System.currentTimeMillis() / 1000);
        payload.put("message", message);
        return GSON.toJson(payload);
    }
}
