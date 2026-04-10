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

package org.damon233.performtrackermod.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.utils.FormattingService;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum ConfigType {
    INT("output_interval", "performtracker.config.output_interval", 5,
        ConfigAccess::getOutputIntervalSeconds,
        v -> ConfigAccess.setOutputIntervalSeconds((Integer) v),
        ConfigAccess::getDefaultOutputIntervalSeconds),

    BOOL("chat_enabled", "performtracker.config.chat_enabled", true,
        ConfigAccess::isChatEnabled,
        v -> ConfigAccess.setChatEnabled((Boolean) v),
        ConfigAccess::getDefaultChatEnabled),

    BOOL2("export_enabled", "performtracker.config.export_enabled", true,
        ConfigAccess::isExportEnabled,
        v -> ConfigAccess.setExportEnabled((Boolean) v),
        ConfigAccess::getDefaultExportEnabled),

    STRING("export_directory", "performtracker.config.export_directory", "performance_data",
        ConfigAccess::getExportDirectory,
        v -> ConfigAccess.setExportDirectory((String) v),
        ConfigAccess::getDefaultExportDirectory),

    STRING2("output_format", "performtracker.config.output_format", "csv",
        ConfigAccess::getOutputFormat,
        v -> ConfigAccess.setOutputFormat((String) v),
        ConfigAccess::getDefaultOutputFormat),

    BOOL4("collect_fps", "performtracker.config.collect_fps", true,
        ConfigAccess::isCollectFps,
        v -> ConfigAccess.setCollectFps((Boolean) v),
        ConfigAccess::getDefaultCollectFps),

    BOOL5("collect_tps", "performtracker.config.collect_tps", true,
        ConfigAccess::isCollectTps,
        v -> ConfigAccess.setCollectTps((Boolean) v),
        ConfigAccess::getDefaultCollectTps),

    BOOL6("collect_mspt", "performtracker.config.collect_mspt", true,
        ConfigAccess::isCollectMspt,
        v -> ConfigAccess.setCollectMspt((Boolean) v),
        ConfigAccess::getDefaultCollectMspt),

    BOOL7("collect_heap", "performtracker.config.collect_heap", true,
        ConfigAccess::isCollectHeap,
        v -> ConfigAccess.setCollectHeap((Boolean) v),
        ConfigAccess::getDefaultCollectHeap),

    BOOL8("collect_cpu", "performtracker.config.collect_cpu", true,
        ConfigAccess::isCollectCpu,
        v -> ConfigAccess.setCollectCpu((Boolean) v),
        ConfigAccess::getDefaultCollectCpu),

    BOOL10("binary_units", "performtracker.config.binary_units", true,
        ConfigAccess::isBinaryUnits,
        v -> ConfigAccess.setBinaryUnits((Boolean) v),
        ConfigAccess::getDefaultBinaryUnits),

    BOOL11("network_enabled", "performtracker.config.network_enabled", false,
        ConfigAccess::isNetworkEnabled,
        v -> ConfigAccess.setNetworkEnabled((Boolean) v),
        ConfigAccess::getDefaultNetworkEnabled),

    STRING4("network_host", "performtracker.config.network_host", "localhost",
        ConfigAccess::getNetworkUrl,
        v -> ConfigAccess.setNetworkUrl((String) v),
        ConfigAccess::getDefaultNetworkUrl),

    INT2("receiver_port", "performtracker.config.receiver_port", 31415,
        ConfigAccess::getReceiverPort,
        v -> ConfigAccess.setReceiverPort((Integer) v),
        ConfigAccess::getDefaultReceiverPort),

    INT3("sender_port", "performtracker.config.sender_port", 31416,
        ConfigAccess::getSenderPort,
        v -> ConfigAccess.setSenderPort((Integer) v),
        ConfigAccess::getDefaultSenderPort);

    public final String key;
    public final String translationKey;
    public final Object defaultValue;
    public final Supplier<Object> getter;
    public final Consumer<Object> setter;
    public final Supplier<Object> defaultGetter;

    ConfigType(String key, String translationKey, Object defaultValue,
               Supplier<Object> getter, Consumer<Object> setter, Supplier<Object> defaultGetter) {
        this.key = key;
        this.translationKey = translationKey;
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
        this.defaultGetter = defaultGetter;
    }

    public void sendSuccess(ServerCommandSource source, Object value) {
        Text name = Text.translatable(translationKey);
        Text val = value instanceof Boolean ?
            Text.translatable(Boolean.TRUE.equals(value) ? "performtracker.config.value.true" : "performtracker.config.value.false") :
            FormattingService.colorValue(String.valueOf(value));
        source.sendFeedback(() -> Text.translatable("performtracker.config.set.success",
            FormattingService.colorLabel(name.getString()), val), false);
    }

    public void sendInfo(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("performtracker.config.info",
            FormattingService.colorLabel(Text.translatable(translationKey).getString()),
            FormattingService.colorValue(String.valueOf(getter.get())),
            FormattingService.colorValue(String.valueOf(defaultGetter.get()))), false);
    }

    public void sendLine(ServerCommandSource source) {
        MutableText current = FormattingService.colorValue(String.valueOf(getter.get()));
        MutableText def = FormattingService.colorValue(String.valueOf(defaultGetter.get()));
        source.sendFeedback(() -> Text.translatable(translationKey).append(": ").append(current).append(" (default: ").append(def).append(")"), false);
    }
}
