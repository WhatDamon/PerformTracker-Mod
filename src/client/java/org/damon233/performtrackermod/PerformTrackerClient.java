package org.damon233.performtrackermod;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ClientTickCollector;
import org.damon233.performtrackermod.config.ConfigAccess;

public class PerformTrackerClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PerformTracker.MOD_ID);
	private static ClientTickCollector clientTickCollector;

	@Override
	public void onInitializeClient() {
		LOGGER.info("PerformTrackerClient initializing...");
		
		clientTickCollector = new ClientTickCollector();
		PerformTracker.setFpsProvider(clientTickCollector);
		
		LOGGER.info("PerformTracker client components ready.");
		if (ConfigAccess.isClothConfigLoaded()) {
			LOGGER.info("Cloth Config API detected - configure via Mod Menu");
		} else {
			LOGGER.warn("Cloth Config API NOT detected - config screen will not be available");
		}
	}

	public static ClientTickCollector getClientTickCollector() {
		return clientTickCollector;
	}
}
