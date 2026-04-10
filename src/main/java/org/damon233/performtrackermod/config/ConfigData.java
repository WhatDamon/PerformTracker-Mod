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

public class ConfigData {
    int outputIntervalSeconds = ConfigDefaults.DEFAULT_OUTPUT_INTERVAL_SECONDS;
    boolean chatEnabled = ConfigDefaults.DEFAULT_CHAT_ENABLED;
    boolean exportEnabled = ConfigDefaults.DEFAULT_EXPORT_ENABLED;
    String exportDirectory = ConfigDefaults.DEFAULT_EXPORT_DIRECTORY;
    String outputFormat = ConfigDefaults.DEFAULT_OUTPUT_FORMAT;
    boolean collectFps = ConfigDefaults.DEFAULT_COLLECT_FPS;
    boolean collectTps = ConfigDefaults.DEFAULT_COLLECT_TPS;
    boolean collectMspt = ConfigDefaults.DEFAULT_COLLECT_MSPT;
    boolean collectHeap = ConfigDefaults.DEFAULT_COLLECT_HEAP;
    boolean collectCpu = ConfigDefaults.DEFAULT_COLLECT_CPU;
    boolean binaryUnits = ConfigDefaults.DEFAULT_BINARY_UNITS;
    boolean networkEnabled = ConfigDefaults.DEFAULT_NETWORK_ENABLED;
    String networkHost = ConfigDefaults.DEFAULT_NETWORK_URL;
    int receiverPort = ConfigDefaults.DEFAULT_RECEIVER_PORT;
    int senderPort = ConfigDefaults.DEFAULT_SENDER_PORT;
}
