package org.damon233.performtrackermod.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonWriter extends MetricsWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BufferedWriter writer;
    private List<String> headers;
    private boolean headerWritten;
    private boolean firstRow;

    public JsonWriter(String directory, String baseName) throws IOException {
        super(directory, baseName, "json");
        this.writer = Files.newBufferedWriter(getFilePath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.headers = List.of();
        this.headerWritten = false;
        this.firstRow = true;
    }

    @Override
    protected String getThreadName() {
        return "PerformTracker-JsonWriter";
    }

    @Override
    public void writeHeader(String... headers) {
        this.headers = List.of(headers);
    }

    @Override
    protected synchronized void writeRowInternal(Object... values) throws IOException {
        if (!headerWritten) {
            writer.write("[\n");
            headerWritten = true;
            firstRow = true;
        }

        if (!firstRow) {
            writer.write(",\n");
        }
        firstRow = false;

        long timestamp = Instant.now().toEpochMilli();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", timestamp);
        for (int i = 0; i < headers.size() && i < values.length; i++) {
            Object v = values[i];
            row.put(headers.get(i), formatValue(v));
        }

        writer.write("  ");
        writer.write(GSON.toJson(row));
        writer.flush();
    }

    private Object formatValue(Object v) {
        if (v instanceof Double d) {
            if (Double.isNaN(d)) return null;
            if (Double.isInfinite(d)) return "infinite";
            return d;
        }
        return v;
    }

    @Override
    protected void onClose() throws IOException {
        if (writer != null) {
            if (headerWritten) {
                writer.write("\n]");
            }
            writer.close();
            writer = null;
        }
    }
}
