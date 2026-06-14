package quark19.chestradar.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import quark19.chestradar.ModConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Storage Highlighter Config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            // Server-side setting entry
            general.addEntry(entryBuilder.startIntField(Component.literal("Scan Radius"), ModConfig.INSTANCE.scanRadius)
                    .setDefaultValue(24)
                    .setMin(8)
                    .setMax(32)
                    .setTooltip(Component.literal("Radius of blocks the mod will search (Client Only)."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.scanRadius = newValue)
                    .build());

            // Save handler
            builder.setSavingRunnable(ModConfig::save);

            return builder.build();
        };
    }
}