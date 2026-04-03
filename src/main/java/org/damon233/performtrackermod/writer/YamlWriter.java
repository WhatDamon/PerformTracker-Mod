package org.damon233.performtrackermod.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

public class YamlWriter extends MetricsWriter {
    private BufferedWriter writer;
    private List<String> headers;
    private boolean headerWritten;
    private int rowCount;

    public YamlWriter(String directory, String baseName) throws IOException {
        super(directory, baseName, "yaml");
        this.writer = Files.newBufferedWriter(getFilePath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.headers = List.of();
        this.headerWritten = false;
        this.rowCount = 0;
    }

    @Override
    protected String getThreadName() {
        return "PerformTracker-YamlWriter";
    }

    @Override
    public void writeHeader(String... headers) {
        this.headers = List.of(headers);
    }

    @Override
    protected synchronized void writeRowInternal(Object... values) throws IOException {
        if (!headerWritten) {
            writer.write("metrics:\n");
            headerWritten = true;
        }

        rowCount++;
        writer.write(String.format("  - id: %d\n", rowCount));
        writer.write(String.format("    timestamp: %d\n", Instant.now().toEpochMilli()));

        for (int i = 0; i < headers.size() && i < values.length; i++) {
            String key = headers.get(i);
            Object v = values[i];
            writer.write(String.format("    %s: %s\n", key, formatValue(v)));
        }
        writer.flush();
    }

    private String formatValue(Object v) {
        if (v instanceof Double d) {
            if (Double.isNaN(d)) return "null";
            if (Double.isInfinite(d)) return ".inf";
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf(d.longValue());
            }
            return String.valueOf(d);
        }
        if (v == null) return "null";
        String str = v.toString();
        if (str.contains(":") || str.contains("#") || str.contains("\n") ||
            str.contains("'") || str.contains("\"") || str.startsWith("-") ||
            str.contains("&") || str.contains("*") || str.contains("!") ||
            str.contains("|") || str.contains(">") || str.equals("...")) {
            return "'" + str.replace("'", "''") + "'";
        }
        return str;
    }

    @Override
    protected void onClose() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
