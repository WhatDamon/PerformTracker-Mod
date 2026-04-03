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

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.collector.SystemInfoCollector;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.controller.TrackerController;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.utils.FormattingService;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PtrackerCommand {
    
    private enum ConfigType {
        INT("output_interval", "performtracker.config.output_interval", 5,
            (Supplier<Object>) ConfigAccess::getOutputIntervalSeconds,
            v -> ConfigAccess.setOutputIntervalSeconds((Integer) v),
            (Supplier<Object>) ConfigAccess::getDefaultOutputIntervalSeconds),
        
        BOOL("chat_enabled", "performtracker.config.chat_enabled", true,
            (Supplier<Object>) ConfigAccess::isChatEnabled,
            v -> ConfigAccess.setChatEnabled((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultChatEnabled),
        
        BOOL2("export_enabled", "performtracker.config.export_enabled", true,
            (Supplier<Object>) ConfigAccess::isExportEnabled,
            v -> ConfigAccess.setExportEnabled((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultExportEnabled),

        STRING("export_directory", "performtracker.config.export_directory", "performance_data",
            (Supplier<Object>) ConfigAccess::getExportDirectory,
            v -> ConfigAccess.setExportDirectory((String) v),
            (Supplier<Object>) ConfigAccess::getDefaultExportDirectory),
        
        BOOL3("network_enabled", "performtracker.config.network_enabled", false,
            (Supplier<Object>) ConfigAccess::isNetworkEnabled,
            v -> ConfigAccess.setNetworkEnabled((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultNetworkEnabled),
        
        STRING2("network_endpoint", "performtracker.config.network_endpoint", "http://localhost:31415",
            (Supplier<Object>) ConfigAccess::getNetworkEndpoint,
            v -> ConfigAccess.setNetworkEndpoint((String) v),
            (Supplier<Object>) ConfigAccess::getDefaultNetworkEndpoint),

        STRING3("output_format", "performtracker.config.output_format", "csv",
            (Supplier<Object>) ConfigAccess::getOutputFormat,
            v -> ConfigAccess.setOutputFormat((String) v),
            (Supplier<Object>) ConfigAccess::getDefaultOutputFormat),

        BOOL4("collect_fps", "performtracker.config.collect_fps", true,
            (Supplier<Object>) ConfigAccess::isCollectFps,
            v -> ConfigAccess.setCollectFps((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectFps),
        
        BOOL5("collect_tps", "performtracker.config.collect_tps", true,
            (Supplier<Object>) ConfigAccess::isCollectTps,
            v -> ConfigAccess.setCollectTps((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectTps),
        
        BOOL6("collect_mspt", "performtracker.config.collect_mspt", true,
            (Supplier<Object>) ConfigAccess::isCollectMspt,
            v -> ConfigAccess.setCollectMspt((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectMspt),
        
        BOOL7("collect_heap", "performtracker.config.collect_heap", true,
            (Supplier<Object>) ConfigAccess::isCollectHeap,
            v -> ConfigAccess.setCollectHeap((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectHeap),
        
        BOOL8("collect_heap", "performtracker.config.collect_heap", true,
            (Supplier<Object>) ConfigAccess::isCollectHeap,
            v -> ConfigAccess.setCollectHeap((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectHeap),

        BOOL9("collect_cpu", "performtracker.config.collect_cpu", true,
            (Supplier<Object>) ConfigAccess::isCollectCpu,
            v -> ConfigAccess.setCollectCpu((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultCollectCpu),
        
        BOOL10("binary_units", "performtracker.config.binary_units", true,
            (Supplier<Object>) ConfigAccess::isBinaryUnits,
            v -> ConfigAccess.setBinaryUnits((Boolean) v),
            (Supplier<Object>) ConfigAccess::getDefaultBinaryUnits);
        
        final String key;
        final String translationKey;
        final Object defaultValue;
        final Supplier<Object> getter;
        final Consumer<Object> setter;
        final Supplier<Object> defaultGetter;
        
        ConfigType(String key, String translationKey, Object defaultValue,
                   Supplier<Object> getter, Consumer<Object> setter, Supplier<Object> defaultGetter) {
            this.key = key;
            this.translationKey = translationKey;
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
            this.defaultGetter = defaultGetter;
        }
        
        void sendSuccess(ServerCommandSource source, Object value) {
            Text name = Text.translatable(translationKey);
            Text val = value instanceof Boolean ? 
                Text.translatable(Boolean.TRUE.equals(value) ? "performtracker.config.value.true" : "performtracker.config.value.false") :
                FormattingService.colorValue(String.valueOf(value));
            source.sendFeedback(() -> Text.translatable("performtracker.config.set.success", 
                FormattingService.colorLabel(name.getString()), val), false);
        }
        
        void sendInfo(ServerCommandSource source) {
            source.sendFeedback(() -> Text.translatable("performtracker.config.info",
                FormattingService.colorLabel(Text.translatable(translationKey).getString()), 
                FormattingService.colorValue(String.valueOf(getter.get())), 
                FormattingService.colorValue(String.valueOf(defaultGetter.get()))), false);
        }
        
        void sendLine(ServerCommandSource source) {
            MutableText current = FormattingService.colorValue(
                Boolean.TRUE.equals(getter.get()) ? "true" : "false");
            MutableText def = FormattingService.colorValue(
                Boolean.TRUE.equals(defaultGetter.get()) ? "true" : "false");
            source.sendFeedback(() -> Text.translatable(translationKey).append(": ").append(current).append(" (default: ").append(def).append(")"), false);
        }
    }
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> ptracker = CommandManager.literal("ptracker")
                    .requires(s -> s.getPlayer() == null || s.getPlayer().isCreativeLevelTwoOp());
            
            ptracker.then(CommandManager.literal("start").executes(ctx -> {
                TrackerController c = PerformTracker.getTrackerController();
                if (c == null) {
                    ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.command.start.error.not_initialized"), false);
                    return 0;
                }
                try {
                    c.start(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.start.success", c.getExportFilePath()), false);
                    return 1;
                } catch (IllegalStateException e) {
                    ctx.getSource().sendFeedback(() -> Text.translatable(e.getMessage().contains("already") ? "performtracker.error.already_running" : "performtracker.error.not_initialized"), false);
                    return 0;
                }
            }));
            
            ptracker.then(CommandManager.literal("stop").executes(ctx -> {
                TrackerController c = PerformTracker.getTrackerController();
                if (c == null) {
                    ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.command.stop.error.not_initialized"), false);
                    return 0;
                }
                try {
                    int count = c.getSampleCount();
                    c.stop();
                    ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.stop.success", count), false);
                    return 1;
                } catch (IllegalStateException e) {
                    ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.error.not_running"), false);
                    return 0;
                }
            }));
            
            LiteralArgumentBuilder<ServerCommandSource> config = CommandManager.literal("config");
            for (ConfigType cfg : ConfigType.values()) {
                LiteralArgumentBuilder<ServerCommandSource> item = CommandManager.literal(cfg.key);
                item.executes(ctx -> {
                    cfg.sendInfo(ctx.getSource());
                    return 1;
                });
                
                if (cfg.defaultValue instanceof Integer) {
                    item.then(CommandManager.argument("value", IntegerArgumentType.integer(1, 3600))
                        .executes(ctx -> {
                            cfg.setter.accept(IntegerArgumentType.getInteger(ctx, "value"));
                            cfg.sendSuccess(ctx.getSource(), cfg.getter.get());
                            return 1;
                        }));
                } else if (cfg.defaultValue instanceof Boolean) {
                    item.then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            cfg.setter.accept(BoolArgumentType.getBool(ctx, "value"));
                            cfg.sendSuccess(ctx.getSource(), cfg.getter.get());
                            return 1;
                        }));
                } else {
                    item.then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(ctx -> {
                            String val = StringArgumentType.getString(ctx, "value");
                            if (cfg == ConfigType.STRING2 && !ConfigAccess.isValidNetworkEndpoint(val)) {
                                ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.config.network_endpoint.error"), false);
                                return 0;
                            }
                            if (cfg == ConfigType.STRING3 && !ConfigAccess.isValidOutputFormat(val)) {
                                ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.config.output_format.error"), false);
                                return 0;
                            }
                            cfg.setter.accept(val);
                            cfg.sendSuccess(ctx.getSource(), cfg.getter.get());
                            return 1;
                        }));
                }
                config.then(item);
            }
            config.executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("=== PerformTracker Config ==="), false);
                for (ConfigType cfg : ConfigType.values()) {
                    cfg.sendLine(ctx.getSource());
                }
                ctx.getSource().sendFeedback(() -> Text.literal("======================="), false);
                ctx.getSource().sendFeedback(() -> Text.translatable("performtracker.config.usage"), false);
                return 1;
            });
            ptracker.then(config);
            
            ptracker.then(CommandManager.literal("deviceinfo").executes(ctx -> {
                SystemInfo info = SystemInfoCollector.collect();

                MutableText title = FormattingService.colorLabel(Text.translatable("performtracker.device.title").getString());
                String unknown = Text.translatable("performtracker.device.unknown").getString();
                MutableText typeLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.type").getString());
                MutableText modelLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.model").getString());
                MutableText cpuLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.cpu").getString());
                MutableText gpuLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.gpu").getString());
                MutableText coresLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.cpu_cores").getString());
                MutableText memLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.memory").getString());
                MutableText osLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.os").getString());
                MutableText javaLabel = FormattingService.colorLabel(Text.translatable("performtracker.device.java").getString());
                String typeValue = Text.translatable("performtracker.device.type." + info.deviceType().getCode()).getString();

                ctx.getSource().sendFeedback(() -> title, false);
                ctx.getSource().sendFeedback(() -> typeLabel.append(FormattingService.colorValue(typeValue)), false);
                ctx.getSource().sendFeedback(() -> modelLabel.append(FormattingService.colorValue(info.deviceModel() != null ? info.deviceModel() : unknown)), false);
                ctx.getSource().sendFeedback(() -> cpuLabel.append(FormattingService.colorValue(info.cpuName() != null ? info.cpuName() : unknown)), false);
                ctx.getSource().sendFeedback(() -> gpuLabel.append(FormattingService.colorValue(info.gpuName() != null ? info.gpuName() : unknown)), false);
                ctx.getSource().sendFeedback(() -> coresLabel.append(FormattingService.colorValue(String.valueOf(info.cpuCores()))), false);
                ctx.getSource().sendFeedback(() -> memLabel.append(FormattingService.colorValue(SystemInfo.formatBytes(info.totalMemoryBytes()))), false);
                ctx.getSource().sendFeedback(() -> osLabel.append(FormattingService.colorValue(info.osName() + " " + info.osVersion() + " (" + info.osArch() + ")")), false);
                ctx.getSource().sendFeedback(() -> javaLabel.append(FormattingService.colorValue(info.javaVersion())), false);

                if (!SystemInfoCollector.isChipRulesValid()) {
                    ctx.getSource().sendFeedback(() -> FormattingService.chatError("performtracker.error.chip_rules_modified"), false);
                }
                return 1;
            }));
            
            dispatcher.register(ptracker);
        });
    }
}
