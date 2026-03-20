package org.damon233.performtrackermod.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import org.damon233.performtrackermod.PerformTracker;
import org.damon233.performtrackermod.controller.TrackerController;
import org.damon233.performtrackermod.utils.TranslationService;

public class PtrackerCommand {
    private static final int PERMISSION_LEVEL = 2;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("ptracker")
                    .requires(source -> {
                        ServerPlayerEntity player = source.getPlayer();
                        if (player != null) {
                            return player.isCreativeLevelTwoOp();
                        }
                        return true;
                    })
                    .then(CommandManager.literal("start")
                        .executes(context -> {
                            TrackerController controller = PerformTracker.getTrackerController();
                            if (controller == null) {
                                context.getSource().sendError(TranslationService.get("error.not_initialized"));
                                return 0;
                            }

                            try {
                                controller.start();
                                String filePath = controller.getCsvFilePath();
                                context.getSource().sendFeedback(
                                    TranslationService.getSupplier("start.success", filePath),
                                    false
                                );
                                return 1;
                            } catch (IllegalStateException e) {
                                context.getSource().sendError(TranslationService.get(e.getMessage().contains("already running") ? "error.already_running" : "error.not_initialized"));
                                return 0;
                            }
                        })
                    )
                    .then(CommandManager.literal("stop")
                        .executes(context -> {
                            TrackerController controller = PerformTracker.getTrackerController();
                            if (controller == null) {
                                context.getSource().sendError(TranslationService.get("error.not_initialized"));
                                return 0;
                            }

                            try {
                                int sampleCount = controller.getSampleCount();
                                controller.stop();
                                context.getSource().sendFeedback(
                                    TranslationService.getSupplier("stop.success", sampleCount),
                                    false
                                );
                                return 1;
                            } catch (IllegalStateException e) {
                                context.getSource().sendError(TranslationService.get("error.not_running"));
                                return 0;
                            }
                        })
                    )
            );
        });
    }
}
