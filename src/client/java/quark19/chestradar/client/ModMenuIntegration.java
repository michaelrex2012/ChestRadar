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

            general.addEntry(entryBuilder.startIntField(Component.literal("Scan Radius"), ModConfig.INSTANCE.scanRadius)
                    .setDefaultValue(24)
                    .setMin(8)
                    .setMax(32)
                    .setTooltip(Component.literal("Radius of blocks the mod will search (Singleplayer or server host only)."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.scanRadius = newValue)
                    .build());

            ConfigCategory rendering = builder.getOrCreateCategory(Component.literal("Rendering"));

            rendering.addEntry(entryBuilder.startBooleanToggle(Component.literal("Slim Outlines"), ModConfig.INSTANCE.slimOutlines)
                    .setDefaultValue(false)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Yes") : Component.literal("No"))
                    .setTooltip(Component.literal("Outlines becomes smaller. Chest outlines look better with this enabled, but outlines on barrels and shulker boxes are not visible"))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.slimOutlines = newValue)
                    .build());

            rendering.addEntry(entryBuilder.startFloatField(Component.literal("Outline Thickness"), ModConfig.INSTANCE.outlineThickness)
                    .setDefaultValue(2)
                    .setMin(1)
                    .setMax(4)
                    .setTooltip(Component.literal("How thick outlines will be."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.outlineThickness = newValue)
                    .build());

            ConfigCategory controls = builder.getOrCreateCategory(Component.literal("Controls"));

            controls.addEntry(entryBuilder.startBooleanToggle(Component.literal("Search Keybind Mode"), ModConfig.INSTANCE.toggleMode)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Toggle") : Component.literal("Hold"))
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Hold or toggle keybind."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.toggleMode = newValue)
                    .build());

            builder.setSavingRunnable(ModConfig::save);

            return builder.build();
        };
    }
}