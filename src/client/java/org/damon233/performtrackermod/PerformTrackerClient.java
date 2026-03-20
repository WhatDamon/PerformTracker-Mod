package org.damon233.performtrackermod;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ClientTickCollector;

public class PerformTrackerClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PerformTracker.MOD_ID);
	private static ClientTickCollector clientTickCollector;

	@Override
	public void onInitializeClient() {
		clientTickCollector = new ClientTickCollector();
		PerformTracker.setFpsProvider(clientTickCollector);
		LOGGER.info("PerformTracker client components ready.");
	}

	public static ClientTickCollector getClientTickCollector() {
		return clientTickCollector;
	}
}
