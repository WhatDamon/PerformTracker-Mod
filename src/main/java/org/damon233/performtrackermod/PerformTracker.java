package org.damon233.performtrackermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ServerMetricsCollector;
import org.damon233.performtrackermod.collector.IFpsProvider;
import org.damon233.performtrackermod.collector.IGpuProvider;
import org.damon233.performtrackermod.config.ConfigAccess;
import org.damon233.performtrackermod.controller.TrackerController;
import org.damon233.performtrackermod.command.PtrackerCommand;
import org.damon233.performtrackermod.network.HttpService;

public class PerformTracker implements ModInitializer {
	public static final String MOD_ID = "performtracker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static TrackerController trackerController;
	private static ServerMetricsCollector serverMetricsCollector;
	private static HttpService httpService;
	private static IFpsProvider fpsProvider;
	private static IGpuProvider gpuProvider;

	@Override
	public void onInitialize() {
		ConfigAccess.init();
		
		LOGGER.info("PerformTracker initialized.");
		serverMetricsCollector = new ServerMetricsCollector();
		trackerController = new TrackerController(serverMetricsCollector, null);
		PtrackerCommand.register();
		
		if (ConfigAccess.isNetworkEnabled()) {
			httpService = new HttpService();
			httpService.initialize(ConfigAccess.getNetworkUrl());
			httpService.tryStartServer();
		}
		
		// Register callback for network enable/disable changes
		ConfigAccess.setOnNetworkEnabledChanged(() -> {
			if (ConfigAccess.isNetworkEnabled()) {
				// Network was enabled - create and start HttpService if not exists
				if (httpService == null) {
					httpService = new HttpService();
					httpService.initialize(ConfigAccess.getNetworkUrl());
				}
				httpService.tryStartServer();
			} else {
				// Network was disabled - stop server and clear HttpService
				if (httpService != null) {
					httpService.stopServer();
					httpService = null;
					LOGGER.info("HttpService unloaded due to network being disabled.");
				}
			}
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (httpService != null) {
				httpService.stopServer();
				LOGGER.info("HttpService server stopped.");
			}
		});
		
		LOGGER.info("PerformTracker server components ready.");
	}

	public static TrackerController getTrackerController() {
		return trackerController;
	}

	public static ServerMetricsCollector getServerMetricsCollector() {
		return serverMetricsCollector;
	}

	public static HttpService getHttpService() {
		return httpService;
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

	public static void setGpuProvider(IGpuProvider provider) {
		PerformTracker.gpuProvider = provider;
	}

	public static IGpuProvider getGpuProvider() {
		return gpuProvider;
	}
}
