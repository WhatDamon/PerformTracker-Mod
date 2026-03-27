package org.damon233.performtrackermod;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ServerMetricsCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.controller.TrackerController;
import org.damon233.performtrackermod.command.PtrackerCommand;

public class PerformTracker implements ModInitializer {
	public static final String MOD_ID = "performtracker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static TrackerController trackerController;
	private static ServerMetricsCollector serverMetricsCollector;
	private static IFpsProvider fpsProvider;

	@Override
	public void onInitialize() {
		LOGGER.info("PerformTracker initialized.");
		serverMetricsCollector = new ServerMetricsCollector();
		trackerController = new TrackerController(serverMetricsCollector, null);
		PtrackerCommand.register();
		LOGGER.info("PerformTracker server components ready.");
	}

	public static TrackerController getTrackerController() {
		return trackerController;
	}

	public static ServerMetricsCollector getServerMetricsCollector() {
		return serverMetricsCollector;
	}

	public static void setFpsProvider(Object fpsProvider) {
		IFpsProvider provider = (IFpsProvider) fpsProvider;
		PerformTracker.fpsProvider = provider;
		if (trackerController != null) {
			trackerController.setFpsProvider(provider);
		}
	}

	public static IFpsProvider getFpsProvider() {
		return fpsProvider;
	}
}
