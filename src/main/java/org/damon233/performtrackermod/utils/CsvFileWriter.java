package org.damon233.performtrackermod.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CsvFileWriter implements AutoCloseable {
    private static final DateTimeFormatter FILENAME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    private final BufferedWriter writer;
    private final Path filePath;
    private boolean headerWritten = false;

    public CsvFileWriter(String directory, String baseName) throws IOException {
        String filename = String.format("%s_%s.csv", baseName, FILENAME_FORMAT.format(Instant.now()));
        this.filePath = Path.of(directory, filename);
        Files.createDirectories(Path.of(directory));
        this.writer = Files.newBufferedWriter(this.filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void writeHeader(String... headers) throws IOException {
        if (headerWritten) return;
        
        StringBuilder sb = new StringBuilder("unix_timestamp");
        for (String h : headers) sb.append(",").append(h);
        writer.write(sb.toString());
        writer.newLine();
        headerWritten = true;
    }

    public void writeRow(Object... values) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Instant.now().toEpochMilli());
        for (Object v : values) {
            sb.append(",");
            if (v != null) {
                String str = v instanceof Double d && Double.isInfinite(d) ? "infinite" : v.toString();
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

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
