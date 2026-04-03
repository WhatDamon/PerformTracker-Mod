package org.damon233.performtrackermod.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.damon233.performtrackermod.collector.SystemInfoCollector;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

public class HttpService {
    private static final Logger LOGGER = LoggerFactory.getLogger("performtracker");
    
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int MAX_QUEUE_SIZE = 50;
    
    private final BlockingQueue<String> sendQueue;
    private final ExecutorService senderExecutor;
    private final AtomicBoolean senderRunning;
    
    private HttpClient httpClient;
    private String remoteUrl;
    
    private HttpServer server;
    private int localPort;
    private final ExecutorService serverExecutor;
    private final AtomicBoolean serverRunning;
    
    public HttpService() {
        this.sendQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformTracker-Sender");
            t.setDaemon(true);
            return t;
        });
        this.senderRunning = new AtomicBoolean(false);
        
        this.serverExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "PerformTracker-Server");
            t.setDaemon(true);
            return t;
        });
        this.serverRunning = new AtomicBoolean(false);
    }
    
    public void initialize(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        this.localPort = ConfigAccess.parsePort(remoteUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }
    
    public synchronized void start() {
        startSender();
    }

    public synchronized boolean tryStartServer() {
        if (serverRunning.get()) {
            return true;
        }
        return startServerInternal();
    }

    public synchronized void startServer() {
        if (!serverRunning.get()) {
            startServerInternal();
        }
    }

    private boolean startServerInternal() {
        try {
            String bindHost = ConfigAccess.getLocalServerHost();
            server = HttpServer.create(new InetSocketAddress(bindHost, localPort), 0);
            server.setExecutor(serverExecutor);

            server.createContext("/api/deviceinfo", new DeviceInfoHandler());

            server.start();
            serverRunning.set(true);
            LOGGER.info("HttpService server started on {}:{}", bindHost, localPort);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to start HttpService server: {}", e.getMessage());
            return false;
        }
    }
    
    private void startSender() {
        if (senderRunning.compareAndSet(false, true)) {
            LOGGER.info("HttpService sender started, target: {}", remoteUrl);
            senderExecutor.execute(this::sendLoop);
        }
    }

    public synchronized void stop() {
        stopSender();
    }

    public synchronized void stopServer() {
        if (server != null && serverRunning.compareAndSet(true, false)) {
            server.stop(0);
            serverExecutor.shutdownNow();
            LOGGER.info("HttpService server stopped");
        }
    }
    
    private void stopSender() {
        if (senderRunning.compareAndSet(true, false)) {
            int dropped = 0;
            while (!sendQueue.isEmpty()) {
                sendQueue.poll();
                dropped++;
            }
            senderExecutor.shutdownNow();
            LOGGER.info("HttpService sender stopped, {} items dropped", dropped);
        }
    }
    
    public void send(String jsonPayload) {
        if (!senderRunning.get()) {
            return;
        }
        
        if (sendQueue.remainingCapacity() == 0) {
            sendQueue.poll();
        }
        
        sendQueue.offer(jsonPayload);
    }
    
    private void sendLoop() {
        while (senderRunning.get()) {
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
                    .uri(URI.create(remoteUrl))
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
    
    public boolean isServerRunning() {
        return serverRunning.get();
    }
    
    public int getLocalPort() {
        return localPort;
    }
    
    private class DeviceInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            SystemInfo info = SystemInfoCollector.collect();
            boolean chipRulesTrusted = SystemInfoCollector.isChipRulesValid();
            String json = String.format(
                "{\"deviceType\":\"%s\",\"deviceModel\":\"%s\",\"cpuName\":\"%s\",\"gpuName\":\"%s\",\"cpuCores\":%d,\"memory\":%d," +
                "\"os\":\"%s\",\"osVersion\":\"%s\",\"osArch\":\"%s\"," +
                "\"javaVersion\":\"%s\",\"minecraftVersion\":\"%s\",\"modVersion\":\"%s\"," +
                "\"chipRulesTrusted\":%b}",
                JsonFormatter.escapeJson(info.deviceType().getCode()),
                JsonFormatter.escapeJson(info.deviceModel() != null ? info.deviceModel() : "Unknown"),
                JsonFormatter.escapeJson(info.cpuName() != null ? info.cpuName() : "Unknown"),
                JsonFormatter.escapeJson(info.gpuName() != null ? info.gpuName() : "Unknown"),
                info.cpuCores(),
                info.totalMemoryBytes(),
                JsonFormatter.escapeJson(info.osName()),
                JsonFormatter.escapeJson(info.osVersion()),
                JsonFormatter.escapeJson(info.osArch()),
                JsonFormatter.escapeJson(info.javaVersion()),
                JsonFormatter.escapeJson(info.minecraftVersion()),
                JsonFormatter.escapeJson(info.modVersion()),
                chipRulesTrusted
            );
            
            sendResponse(exchange, 200, json);
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String body) {
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to send response: {}", e.getMessage());
            }
        }
    }
}
