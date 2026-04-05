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

package org.damon233.performtrackermod.network;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.collector.SystemInfoCollector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpServerManager {
    private static final HttpServerManager INSTANCE = new HttpServerManager();
    public static HttpServerManager getInstance() {
        return INSTANCE;
    }

    private HttpServer server;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private volatile String currentHost = null;
    private volatile int currentPort = -1;

    private HttpServerManager() {
    }

    public void start(String host, int port) {
        if (running && currentHost != null && currentHost.equals(host) && currentPort == port) {
            PerformTracker.LOGGER.debug("HTTP server already running on {}:{}", host, port);
            return;
        }

        stop();

        try {
            InetAddress address = InetAddress.getByName(host);
            server = HttpServer.create(new InetSocketAddress(address, port), 0);
            server.createContext("/api/deviceinfo", new DeviceInfoHandler());

            executorService = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "PerformTracker-HttpServer");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
            server.setExecutor(executorService);

            server.start();
            running = true;
            currentHost = host;
            currentPort = port;
            PerformTracker.LOGGER.info("HTTP server started on {}:{}", host, port);
        } catch (IOException e) {
            PerformTracker.LOGGER.error("Failed to start HTTP server on {}:{}: {}", host, port, e.getMessage());
            running = false;
            currentHost = null;
            currentPort = -1;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
        running = false;
        currentHost = null;
        currentPort = -1;
        PerformTracker.LOGGER.info("HTTP server stopped");
    }

    public void restart(String newHost, int newPort) {
        stop();
        start(newHost, newPort);
    }

    private static class DeviceInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            URI requestUri = exchange.getRequestURI();
            String path = requestUri != null ? requestUri.getPath() : "";
            if (!"/api/deviceinfo".equals(path)) {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                return;
            }

            try {
                var systemInfo = SystemInfoCollector.collect();
                boolean chipRulesTrusted = SystemInfoCollector.isChipRulesValid();
                String response = JsonFormatter.formatDeviceInfo(systemInfo, chipRulesTrusted);
                sendResponse(exchange, 200, response);
            } catch (Exception e) {
                PerformTracker.LOGGER.error("Error handling deviceinfo request: {}", e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
                os.flush();
            }
        } catch (IOException e) {
            PerformTracker.LOGGER.error("Failed to send response: {}", e.getMessage());
        }
    }
}
