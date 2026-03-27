package org.damon233.performtrackermod.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClothConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("performtracker.config.title"));
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.general"));
        
        general.addEntry(entryBuilder.startIntField(
                Text.translatable("performtracker.config.output_interval"),
                ConfigAccess.getOutputIntervalSeconds())
                .setDefaultValue(5)
                .setMin(1)
                .setMax(3600)
                .setSaveConsumer(ConfigAccess::setOutputIntervalSeconds)
                .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("performtracker.config.chat_enabled"),
                ConfigAccess.isChatEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigAccess::setChatEnabled)
                .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("performtracker.config.csv_enabled"),
                ConfigAccess.isCsvEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigAccess::setCsvEnabled)
                .build());
        
        general.addEntry(entryBuilder.startTextField(
                Text.translatable("performtracker.config.csv_directory"),
                ConfigAccess.getCsvDirectory())
                .setDefaultValue("performance_data")
                .setSaveConsumer(ConfigAccess::setCsvDirectory)
                .build());

        ConfigCategory metrics = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.metrics"));

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_fps"),
                        ConfigAccess.isCollectFps())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigAccess::setCollectFps)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_tps"),
                        ConfigAccess.isCollectTps())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigAccess::setCollectTps)
                .build());

        metrics.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("performtracker.config.collect_mspt"),
                        ConfigAccess.isCollectMspt())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigAccess::setCollectMspt)
                .build());
        
        ConfigCategory network = builder.getOrCreateCategory(Text.translatable("performtracker.config.category.network"));
        
        network.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("performtracker.config.network_enabled"),
                ConfigAccess.isNetworkEnabled())
                .setDefaultValue(false)
                .setSaveConsumer(ConfigAccess::setNetworkEnabled)
                .build());
        
        network.addEntry(entryBuilder.startTextField(
                Text.translatable("performtracker.config.network_url"),
                ConfigAccess.getNetworkUrl())
                .setDefaultValue("http://localhost:31415/api/metrics")
                .setSaveConsumer(ConfigAccess::setNetworkUrl)
                .build());
        
        return builder.build();
    }
}
