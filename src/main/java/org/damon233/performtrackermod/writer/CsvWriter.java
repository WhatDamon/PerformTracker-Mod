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
