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

package org.damon233.performtrackermod.config;

import java.util.regex.Pattern;

public class ConfigDefaults {
    public static final int DEFAULT_OUTPUT_INTERVAL_SECONDS = 5;
    public static final boolean DEFAULT_CHAT_ENABLED = true;
    public static final boolean DEFAULT_EXPORT_ENABLED = true;
    public static final String DEFAULT_EXPORT_DIRECTORY = "performance_data";
    public static final String DEFAULT_OUTPUT_FORMAT = "csv";
    public static final boolean DEFAULT_COLLECT_FPS = true;
    public static final boolean DEFAULT_COLLECT_TPS = true;
    public static final boolean DEFAULT_COLLECT_MSPT = true;
    public static final boolean DEFAULT_COLLECT_HEAP = true;
    public static final boolean DEFAULT_COLLECT_CPU = true;
    public static final boolean DEFAULT_BINARY_UNITS = true;
    public static final boolean DEFAULT_NETWORK_ENABLED = false;
    public static final String DEFAULT_NETWORK_URL = "localhost";
    public static final int DEFAULT_RECEIVER_PORT = 31415;
    public static final int DEFAULT_SENDER_PORT = 31416;
    public static final Pattern RECEIVER_HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*$");
}
