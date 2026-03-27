package org.damon233.performtrackermod;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ClientMetricsCollector;
import org.damon233.performtrackermod.config.ConfigAccess;

public class PerformTrackerClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PerformTracker.MOD_ID);
	private static ClientMetricsCollector clientMetricsCollector;

	@Override
	public void onInitializeClient() {
		LOGGER.info("PerformTrackerClient initializing...");
		
		ConfigAccess.init();
		
		clientMetricsCollector = new ClientMetricsCollector();
		PerformTracker.setFpsProvider(clientMetricsCollector);
		
		LOGGER.info("PerformTracker client components ready.");
		if (ConfigAccess.isClothConfigLoaded()) {
			LOGGER.info("Cloth Config API detected - configure via Mod Menu");
		} else {
			LOGGER.warn("Cloth Config API NOT detected - config screen will not be available");
		}
	}
}
