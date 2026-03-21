package org.damon233.performtrackermod.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP POST sender for transmitting performance data.
 * Uses a background thread to send data without blocking the server tick.
 */
public class HttpSender {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 30000;
    
    private final BlockingQueue<String> sendQueue;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicInteger consecutiveFailures;
    
    private String url;
    
    public HttpSender() {
        this.sendQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformTracker-HttpSender");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.consecutiveFailures = new AtomicInteger(0);
        this.url = "";
    }
    
    /**
     * Initialize the sender with the target URL.
     * Must be called before start().
     */
    public void initialize(String url) {
        this.url = url;
    }
    
    /**
     * Start the background sender thread.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("HttpSender started, target URL: {}", url);
            executor.execute(this::sendLoop);
        }
    }
    
    /**
     * Stop the sender and wait for pending sends.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("HttpSender stopping...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("HttpSender stopped");
        }
    }
    
    /**
     * Queue a JSON payload to be sent.
     * Non-blocking, will drop oldest items if queue is full.
     */
    public void send(String jsonPayload) {
        if (!running.get()) {
            return;
        }
        
        // Remove oldest item if queue is full
        if (sendQueue.size() >= MAX_QUEUE_SIZE) {
            sendQueue.poll();
            LOGGER.warn("Send queue full, dropping oldest item");
        }
        
        sendQueue.offer(jsonPayload);
    }
    
    /**
     * Check if the sender is connected (can send).
     */
    public boolean isConnected() {
        return running.get() && consecutiveFailures.get() < MAX_RETRY_ATTEMPTS;
    }
    
    /**
     * Main send loop running in background thread.
     */
    private void sendLoop() {
        while (running.get()) {
            try {
                String payload = sendQueue.poll(1, TimeUnit.SECONDS);
                if (payload != null) {
                    sendWithRetry(payload);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Drain remaining items
        while (!sendQueue.isEmpty()) {
            String payload = sendQueue.poll();
            if (payload != null) {
                sendSync(payload);
            }
        }
    }
    
    /**
     * Send with retry on failure.
     */
    private void sendWithRetry(String payload) {
        int attempts = 0;
        long delay = INITIAL_RETRY_DELAY_MS;
        
        while (attempts < MAX_RETRY_ATTEMPTS && running.get()) {
            try {
                boolean success = sendSync(payload);
                if (success) {
                    consecutiveFailures.set(0);
                    return;
                }
            } catch (Exception e) {
                LOGGER.debug("Send attempt {} failed: {}", attempts + 1, e.getMessage());
            }
            
            attempts++;
            if (attempts < MAX_RETRY_ATTEMPTS && running.get()) {
                try {
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        
        consecutiveFailures.incrementAndGet();
        LOGGER.warn("Failed to send after {} attempts", MAX_RETRY_ATTEMPTS);
    }
    
    /**
     * Synchronous HTTP POST.
     */
    private boolean sendSync(String payload) {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            // Write payload
            try (var os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.debug("Sent successfully, response code: {}", responseCode);
                return true;
            } else {
                LOGGER.warn("Server returned error code: {}", responseCode);
                return false;
            }
            
        } catch (IOException e) {
            LOGGER.debug("Connection failed: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
