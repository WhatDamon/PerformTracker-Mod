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
import org.damon233.performtrackermod.collector.system.SystemInfoCollector;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.controller.TrackerController;
import org.damon233.performtrackermod.data.SystemInfo;
import org.damon233.performtrackermod.utils.FormattingService;

public class PtrackerCommand {

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
                            if (cfg == ConfigType.STRING2 && !ConfigAccess.isValidOutputFormat(val)) {
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
