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

package org.damon233.performtrackermod.controller;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.data.PerformanceMetrics;
import org.damon233.performtrackermod.utils.FormattingService;

public class ChatMessageBuilder {
    private final MutableText[] chatMessageParts = new MutableText[10];
    private int chatMessagePartCount;

    public MutableText build(PerformanceMetrics metrics) {
        chatMessagePartCount = 0;
        if (ConfigAccess.isCollectFps()) {
            addPart("FPS: ", String.format("%.1f", metrics.fps()));
        }
        if (ConfigAccess.isCollectTps()) {
            addPart("TPS: ", PerformanceMetrics.formatValue(metrics.tps()));
        }
        if (ConfigAccess.isCollectMspt()) {
            addPart("MSPT: ", PerformanceMetrics.formatValue(metrics.mspt()));
        }
        if (ConfigAccess.isCollectHeap()) {
            addPart("Heap: ", PerformanceMetrics.formatMemoryMB(metrics.heapUsed()) + " / " + PerformanceMetrics.formatMemoryMB(metrics.heapMax()));
        }
        if (ConfigAccess.isCollectCpu()) {
            addPart("CPU: ", String.format("%.1f%%", metrics.cpuUsage()));
        }

        MutableText result = Text.empty();
        for (int i = 0; i < chatMessagePartCount; i++) {
            if (i > 0) {
                result.append(Text.literal(" | ").withColor(0x888888));
            }
            result.append(chatMessageParts[i]);
        }
        return result;
    }

    private void addPart(String label, String value) {
        if (chatMessagePartCount >= chatMessageParts.length) {
            return;
        }
        chatMessageParts[chatMessagePartCount++] = Text.literal(label).withColor(0x888888).append(FormattingService.colorValue(value));
    }
}
