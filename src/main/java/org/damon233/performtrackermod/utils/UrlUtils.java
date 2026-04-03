package org.damon233.performtrackermod.utils;

import java.net.URI;

public class UrlUtils {
    public static int parsePort(String url) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            if (port != -1) {
                return port;
            }
        } catch (Exception ignored) {
        }
        return 31415;
    }

    public static String parseHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (Exception ignored) {
        }
        return "localhost";
    }
}
