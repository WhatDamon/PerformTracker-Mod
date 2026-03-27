package org.damon233.performtrackermod.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * Mod Menu integration for PerformTracker.
 * Provides a config screen accessible from the Mod Menu.
 */
public class PerformTrackerModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClothConfigScreen::createConfigScreen;
    }
}
