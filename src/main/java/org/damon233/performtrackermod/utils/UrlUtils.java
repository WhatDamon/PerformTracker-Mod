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

package org.damon233.performtrackermod.utils;

import java.net.URI;

public class UrlUtils {
    private static final String[] LOCALHOST_HOSTS = {
        "localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1"
    };
    
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
    
    public static boolean isLocalhost(String host) {
        if (host == null) {
            return true;
        }
        String lowerHost = host.toLowerCase();
        for (String localhost : LOCALHOST_HOSTS) {
            if (localhost.equals(lowerHost)) {
                return true;
            }
        }
        return false;
    }
}
