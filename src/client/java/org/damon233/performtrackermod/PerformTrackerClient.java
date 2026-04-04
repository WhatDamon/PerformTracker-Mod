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

package org.damon233.performtrackermod;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.damon233.performtrackermod.collector.ClientGpuCollector;
import org.damon233.performtrackermod.collector.ClientMetricsCollector;
import org.damon233.performtrackermod.config.ConfigAccess;

public class PerformTrackerClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PerformTracker.MOD_ID);

    @Override
	public void onInitializeClient() {
		LOGGER.info("PerformTrackerClient initializing...");
		
		ConfigAccess.init();

        ClientMetricsCollector clientMetricsCollector = new ClientMetricsCollector();
		PerformTracker.setFpsProvider(clientMetricsCollector);

        ClientGpuCollector clientGpuCollector = new ClientGpuCollector();
		PerformTracker.setGpuProvider(clientGpuCollector);
		LOGGER.info("GPU: {}", clientGpuCollector.getGpuName());
		
		LOGGER.info("PerformTracker client components ready.");
		if (ConfigAccess.isClothConfigLoaded()) {
			LOGGER.info("Cloth Config API detected - configure via Mod Menu");
		} else {
			LOGGER.warn("Cloth Config API NOT detected - config screen will not be available");
		}
	}
}
