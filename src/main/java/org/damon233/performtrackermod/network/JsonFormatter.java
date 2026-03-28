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
