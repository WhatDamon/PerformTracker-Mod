package org.damon233.performtrackermod.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvWriter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final DateTimeFormatter FILENAME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
    private static final int MAX_QUEUE_SIZE = 100;

    private final BlockingQueue<Object[]> writeQueue;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    
    private BufferedWriter writer;
    private Path filePath;
    private boolean headerWritten;

    public CsvWriter(String directory, String baseName) throws IOException {
        this.writeQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformTracker-CsvWriter");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.headerWritten = false;
        initFile(directory, baseName);
    }

    private void initFile(String directory, String baseName) throws IOException {
        String filename = String.format("%s_%s.csv", baseName, FILENAME_FORMAT.format(Instant.now()));
        this.filePath = Path.of(directory, filename);
        Files.createDirectories(Path.of(directory));
        this.writer = Files.newBufferedWriter(this.filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void writeHeader(String... headers) throws IOException {
        if (headerWritten || writer == null) return;
        
        StringBuilder sb = new StringBuilder("unix_timestamp");
        for (String h : headers) sb.append(",").append(h);
        writer.write(sb.toString());
        writer.newLine();
        headerWritten = true;
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("CsvWriter started");
            executor.execute(this::writeLoop);
        }
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            writeQueue.clear();
            executor.shutdownNow();
            LOGGER.info("CsvWriter stopped");
        }
    }

    public void enqueue(Object... values) {
        if (running.get()) {
            if (writeQueue.remainingCapacity() == 0) {
                writeQueue.poll();
            }
            writeQueue.offer(values);
        }
    }

    private void writeLoop() {
        while (running.get()) {
            try {
                Object[] values = writeQueue.poll(1, TimeUnit.SECONDS);
                if (values != null && writer != null) {
                    writeRowInternal(values);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                LOGGER.error("Failed to write CSV row", e);
            }
        }
    }

    private void writeRowInternal(Object... values) throws IOException {
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
        stop();
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }
}
