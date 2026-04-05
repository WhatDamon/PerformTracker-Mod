package org.damon233.performtrackermod.network;

import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.config.ConfigAccess;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpMetricsClient {
    private static final UdpMetricsClient INSTANCE = new UdpMetricsClient();
    public static UdpMetricsClient getInstance() {
        return INSTANCE;
    }

    private static final int MAX_QUEUE_SIZE = 50;
    private static final int SOCKET_TIMEOUT_MS = 5000;

    private final BlockingQueue<byte[]> sendQueue;
    private final ExecutorService senderExecutor;
    private final AtomicBoolean senderRunning;

    private volatile InetAddress remoteAddress;
    private volatile int remotePort;
    private volatile String currentHostname;
    private volatile DatagramSocket socket;

    private UdpMetricsClient() {
        this.sendQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformTracker-MetricsSender");
            t.setDaemon(true);
            return t;
        });
        this.senderRunning = new AtomicBoolean(false);
    }

    public void postMetrics(String jsonPayload) {
        if (!ConfigAccess.isNetworkEnabled()) {
            return;
        }

        String baseUrl = ConfigAccess.getNetworkUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }

        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

        if (sendQueue.remainingCapacity() == 0) {
            sendQueue.poll();
        }
        sendQueue.offer(payloadBytes);

        if (senderRunning.compareAndSet(false, true)) {
            resolveEndpoint(baseUrl, ConfigAccess.getSenderPort());
            senderExecutor.execute(this::sendLoop);
        }
    }

    private void resolveEndpoint(String hostname, int port) {
        try {
            this.remoteAddress = InetAddress.getByName(hostname);
            this.remotePort = port;
            this.currentHostname = hostname;
            closeSocket();
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        } catch (Exception e) {
            PerformTracker.LOGGER.warn("Failed to resolve endpoint {}:{}: {}", hostname, port, e.getMessage());
            this.remoteAddress = null;
            this.currentHostname = null;
            this.remotePort = port;
            closeSocket();
            senderRunning.set(false);
        }
    }

    private void sendLoop() {
        while (senderRunning.get()) {
            try {
                byte[] payload = sendQueue.poll(1, TimeUnit.SECONDS);
                if (payload == null) {
                    continue;
                }

                String hostname = ConfigAccess.getNetworkUrl();
                int port = ConfigAccess.getSenderPort();

                if (remoteAddress == null || !hostname.equals(currentHostname) || port != remotePort) {
                    resolveEndpoint(hostname, port);
                }

                if (remoteAddress != null && socket != null) {
                    sendPayload(payload);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        closeSocket();
        senderRunning.set(false);
    }

    private void sendPayload(byte[] jsonPayload) {
        if (socket == null || remoteAddress == null) {
            return;
        }

        try {
            DatagramPacket packet = new DatagramPacket(jsonPayload, jsonPayload.length, remoteAddress, remotePort);
            socket.send(packet);
        } catch (Exception e) {
            PerformTracker.LOGGER.warn("UDP send failed: {}", e.getMessage());
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

    public void close() {
        if (senderRunning.compareAndSet(true, false)) {
            int dropped = 0;
            while (!sendQueue.isEmpty()) {
                sendQueue.poll();
                dropped++;
            }
            senderExecutor.shutdownNow();
            closeSocket();
            this.currentHostname = null;
            PerformTracker.LOGGER.info("UdpMetricsClient stopped, {} items dropped", dropped);
        }
    }
}
