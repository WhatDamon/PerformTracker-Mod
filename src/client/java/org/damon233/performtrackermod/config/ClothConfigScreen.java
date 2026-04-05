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

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.Optional;

public class ClothConfigScreen {
    
    @Environment(EnvType.CLIENT)
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("performtracker.config.title"));
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.general"));
        
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("performtracker.config.output_interval"),
                ConfigAccess.getOutputIntervalSeconds())
                .setDefaultValue(5)
                .setMin(1)
                .setMax(3600)
                .setTooltip(Text.translatable("performtracker.config.output_interval.tooltip"))
                .setSaveConsumer(ConfigAccess::setOutputIntervalSeconds)
                .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("performtracker.config.chat_enabled"),
                ConfigAccess.isChatEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.chat_enabled.tooltip"))
                .setSaveConsumer(ConfigAccess::setChatEnabled)
                .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("performtracker.config.export_enabled"),
                ConfigAccess.isExportEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.export_enabled.tooltip"))
                .setSaveConsumer(ConfigAccess::setExportEnabled)
                .build());

        general.addEntry(entryBuilder.startSelector(
                        Text.translatable("performtracker.config.output_format"),
                        new String[]{"csv", "json", "yaml"},
                        ConfigAccess.getOutputFormat())
                .setDefaultValue("csv")
                .setTooltip(Text.translatable("performtracker.config.output_format.tooltip"))
                .setSaveConsumer(ConfigAccess::setOutputFormat)
                .build());
        
        general.addEntry(entryBuilder.startTextField(
                Text.translatable("performtracker.config.export_directory"),
                ConfigAccess.getExportDirectory())
                .setDefaultValue("performance_data")
                .setTooltip(Text.translatable("performtracker.config.export_directory.tooltip"))
                .setSaveConsumer(ConfigAccess::setExportDirectory)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.binary_units"),
                        ConfigAccess.isBinaryUnits())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.binary_units.tooltip"))
                .setSaveConsumer(ConfigAccess::setBinaryUnits)
                .build());

        ConfigCategory metrics = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.metrics"));

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_fps"),
                        ConfigAccess.isCollectFps())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.collect_fps.tooltip"))
                .setSaveConsumer(ConfigAccess::setCollectFps)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_tps"),
                        ConfigAccess.isCollectTps())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.collect_tps.tooltip"))
                .setSaveConsumer(ConfigAccess::setCollectTps)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_mspt"),
                        ConfigAccess.isCollectMspt())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.collect_mspt.tooltip"))
                .setSaveConsumer(ConfigAccess::setCollectMspt)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_heap"),
                        ConfigAccess.isCollectHeap())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.collect_heap.tooltip"))
                .setSaveConsumer(ConfigAccess::setCollectHeap)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_cpu"),
                        ConfigAccess.isCollectCpu())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("performtracker.config.collect_cpu.tooltip"))
                .setSaveConsumer(ConfigAccess::setCollectCpu)
                .build());

        ConfigCategory network = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.network"));

        network.addEntry(entryBuilder.startTextDescription(
                        Text.translatable("performtracker.config.network_warning")
                        )
                .build());

        network.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.network_enabled"),
                        ConfigAccess.isNetworkEnabled())
                .setDefaultValue(false)
                .setTooltip(Text.translatable("performtracker.config.network_enabled.tooltip"))
                .setSaveConsumer(ConfigAccess::setNetworkEnabled)
                .build());

        network.addEntry(entryBuilder.startTextField(
                        Text.translatable("performtracker.config.network_host"),
                        ConfigAccess.getNetworkUrl())
                .setDefaultValue("localhost")
                .setTooltip(Text.translatable("performtracker.config.network_host.tooltip"))
                .setSaveConsumer(newValue -> {
                    if (ConfigAccess.isValidNetworkUrl(newValue)) {
                        ConfigAccess.setNetworkUrl(newValue);
                    }
                })
                .setErrorSupplier(newValue -> {
                    if (!ConfigAccess.isValidNetworkUrl(newValue)) {
                        return Optional.of(Text.translatable("performtracker.config.network_host.error"));
                    }
                    return Optional.empty();
                })
                .build());

        network.addEntry(entryBuilder.startIntField(
                        Text.translatable("performtracker.config.receiver_port"),
                        ConfigAccess.getReceiverPort())
                .setDefaultValue(31415)
                .setMin(1)
                .setMax(65535)
                .setTooltip(Text.translatable("performtracker.config.receiver_port.tooltip"))
                .setSaveConsumer(port -> {
                    if (port >= 1 && port <= 65535) {
                        ConfigAccess.setReceiverPort(port);
                    }
                })
                .build());

        network.addEntry(entryBuilder.startIntField(
                        Text.translatable("performtracker.config.sender_port"),
                        ConfigAccess.getSenderPort())
                .setDefaultValue(31416)
                .setMin(1)
                .setMax(65535)
                .setTooltip(Text.translatable("performtracker.config.sender_port.tooltip"))
                .setSaveConsumer(port -> {
                    if (port >= 1 && port <= 65535) {
                        ConfigAccess.setSenderPort(port);
                    }
                })
                .build());
        
        return builder.build();
    }
}
