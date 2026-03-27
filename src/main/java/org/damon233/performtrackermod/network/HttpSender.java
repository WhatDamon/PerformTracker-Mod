package org.damon233.performtrackermod.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpSender {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int MAX_QUEUE_SIZE = 50;
    
    private final BlockingQueue<String> sendQueue;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    
    private HttpClient httpClient;
    private String url;
    
    public HttpSender() {
        this.sendQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformTracker-HttpSender");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
    }
    
    public void initialize(String url) {
        this.url = url;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }
    
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("HttpSender started, target URL: {}", url);
            executor.execute(this::sendLoop);
        }
    }
    
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            int dropped = 0;
            while (!sendQueue.isEmpty()) {
                sendQueue.poll();
                dropped++;
            }
            executor.shutdownNow();
            LOGGER.info("HttpSender stopped, {} items dropped", dropped);
        }
    }
    
    public void send(String jsonPayload) {
        if (!running.get()) {
            return;
        }
        
        if (sendQueue.remainingCapacity() == 0) {
            sendQueue.poll();
        }
        
        sendQueue.offer(jsonPayload);
    }
    
    private void sendLoop() {
        while (running.get()) {
            try {
                String payload = sendQueue.poll(1, TimeUnit.SECONDS);
                if (payload != null) {
                    sendPayload(payload);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void sendPayload(String jsonPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.debug("Sent successfully, response: {}", response.statusCode());
            } else {
                LOGGER.warn("Server returned error code: {}", response.statusCode());
            }
        } catch (Exception e) {
            LOGGER.debug("Send failed: {}", e.getMessage());
        }
    }
}
