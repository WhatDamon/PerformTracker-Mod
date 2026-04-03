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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class CsvWriter extends MetricsWriter {
    private BufferedWriter writer;
    private boolean headerWritten;

    public CsvWriter(String directory, String baseName) throws IOException {
        super(directory, baseName, "csv");
        this.writer = Files.newBufferedWriter(getFilePath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.headerWritten = false;
    }

    @Override
    protected String getThreadName() {
        return "PerformTracker-CsvWriter";
    }

    @Override
    public void writeHeader(String... headers) throws IOException {
        if (headerWritten || writer == null) return;

        StringBuilder sb = new StringBuilder("unix_timestamp");
        for (String h : headers) sb.append(",").append(h);
        writer.write(sb.toString());
        writer.newLine();
        headerWritten = true;
    }

    @Override
    protected void writeRowInternal(Object... values) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Instant.now().toEpochMilli());
        for (Object v : values) {
            sb.append(",");
            if (v != null && !(v instanceof Double d && Double.isNaN(d))) {
                String str = v instanceof Double dd && Double.isInfinite(dd) ? "infinite" : v.toString();
                if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                    sb.append("\"").append(str.replace("\"", "\"\"")).append("\"");
                } else {
                    sb.append(str);
                }
            }
        }
        writer.write(sb.toString());
        writer.newLine();
    }

    @Override
    protected void flushInternal() throws IOException {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    protected void onClose() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }
}
