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
    }

    @Override
    protected void flushInternal() throws IOException {
        if (writer != null) {
            writer.flush();
        }
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
            writer.flush();
            writer.close();
            writer = null;
        }
    }
}
