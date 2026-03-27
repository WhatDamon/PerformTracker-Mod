package org.damon233.performtrackermod.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A buffered CSV file writer for performance data logging.
 * Uses BufferedWriter for efficient I/O operations.
 *
 * <p>The writer automatically adds a Unix timestamp as the first column
 * for each row of data.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (CsvFileWriter writer = new CsvFileWriter("performance_data", "metrics")) {
 *     writer.writeHeader("fps", "tps", "mspt");
 *     writer.writeRow(fps, tps, mspt);
 * } // Auto-closes and flushes
 * }</pre>
 *
 * @author PerformTracker
 * @since 1.0.0
 */
public class CsvFileWriter implements AutoCloseable {
    private static final DateTimeFormatter FILENAME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    private final BufferedWriter writer;
    private final Path filePath;
    private boolean headerWritten = false;

    /**
     * Creates a new CSV writer with a timestamped filename.
     *
     * @param directory the directory to create the file in
     * @param baseName  the base name for the file (timestamp will be appended)
     * @throws IOException if the file cannot be created
     */
    public CsvFileWriter(String directory, String baseName) throws IOException {
        String timestamp = FILENAME_FORMAT.format(Instant.now());
        String filename = String.format("%s_%s.csv", baseName, timestamp);
        this.filePath = Path.of(directory, filename);
        
        // Ensure directory exists
        Files.createDirectories(Path.of(directory));
        
        this.writer = Files.newBufferedWriter(
            this.filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    /**
     * Creates a new CSV writer with a custom filename.
     *
     * @param customFilename the full filename (including path)
     * @throws IOException if the file cannot be created
     */
    public CsvFileWriter(String customFilename) throws IOException {
        Path path = Path.of(customFilename);
        
        // Ensure parent directory exists
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        
        this.filePath = path;
        this.writer = Files.newBufferedWriter(
            this.filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    /**
     * Writes the header row to the CSV file.
     * Should be called once before writing data rows.
     *
     * @param headers the column headers
     * @throws IOException if writing fails
     */
    public void writeHeader(String... headers) throws IOException {
        if (!headerWritten) {
            StringBuilder sb = new StringBuilder();
            sb.append("unix_timestamp");
            for (String header : headers) {
                sb.append(",");
                sb.append(header);
            }
            writer.write(sb.toString());
            writer.newLine();
            headerWritten = true;
        }
    }

    /**
     * Writes the header row to the CSV file using a list.
     *
     * @param headers the column headers as a list
     * @throws IOException if writing fails
     */
    public void writeHeader(List<String> headers) throws IOException {
        writeHeader(headers.toArray(new String[0]));
    }

    /**
     * Writes a data row to the CSV file.
     * The Unix timestamp is automatically added as the first column.
     *
     * @param values the values to write
     * @throws IOException if writing fails
     */
    public void writeRow(Object... values) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Instant.now().toEpochMilli());
        
        for (Object value : values) {
            sb.append(",");
            if (value != null) {
                String strValue;
                if (value instanceof Double d && Double.isInfinite(d)) {
                    strValue = "infinite";
                } else {
                    strValue = value.toString();
                }
                if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                    sb.append("\"").append(strValue.replace("\"", "\"\"")).append("\"");
                } else {
                    sb.append(strValue);
                }
            }
        }
        
        writer.write(sb.toString());
        writer.newLine();
    }

    /**
     * Writes a data row to the CSV file using a list.
     *
     * @param values the values to write as a list
     * @throws IOException if writing fails
     */
    public void writeRow(List<Object> values) throws IOException {
        writeRow(values.toArray());
    }

    /**
     * Flushes any buffered data to the file.
     *
     * @throws IOException if flushing fails
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Returns the path of the file being written.
     *
     * @return the file path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Closes the writer and flushes any remaining data.
     *
     * @throws IOException if closing fails
     */
    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
