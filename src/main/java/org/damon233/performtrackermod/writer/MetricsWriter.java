package org.damon233.performtrackermod.writer;

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

public abstract class MetricsWriter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    private static final int MAX_QUEUE_SIZE = 100;
    protected static final DateTimeFormatter FILENAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    protected final BlockingQueue<Object[]> writeQueue;
    protected final ExecutorService executor;
    protected final AtomicBoolean running;
    protected final Path filePath;

    protected MetricsWriter(String directory, String baseName, String extension) throws IOException {
        this.writeQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, getThreadName());
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        String filename = String.format("%s_%s.%s", baseName, FILENAME_FORMAT.format(Instant.now()), extension);
        this.filePath = Path.of(directory, filename);
        Files.createDirectories(Path.of(directory));
    }

    public Path getFilePath() {
        return filePath;
    }

    protected abstract String getThreadName();

    public abstract void writeHeader(String... headers) throws IOException;

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("{} started", getClass().getSimpleName());
            executor.execute(this::writeLoop);
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

    protected abstract void writeRowInternal(Object... values) throws IOException;

    protected void writeLoop() {
        while (running.get()) {
            try {
                Object[] values = writeQueue.poll(1, TimeUnit.SECONDS);
                if (values != null) {
                    writeRowInternal(values);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                LOGGER.error("Failed to write row", e);
            }
        }
    }

    public void flush() {
    }

    @Override
    public void close() {
        stop();
        try {
            onClose();
        } catch (IOException e) {
            LOGGER.error("Failed to close writer", e);
        }
    }

    protected abstract void onClose() throws IOException;

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            writeQueue.clear();
            executor.shutdownNow();
            LOGGER.info("{} stopped", getClass().getSimpleName());
        }
    }
}
