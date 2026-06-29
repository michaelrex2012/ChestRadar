package quark19.chestradar.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import quark19.chestradar.ModConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Chest Radar Config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory features = builder.getOrCreateCategory(Component.literal("Features"));

            features.setDescription(new FormattedText[]{FormattedText.of("Base functionality behaviour")});

            features.addEntry(entryBuilder.startBooleanToggle(Component.literal("Mod Functionality"), ModConfig.INSTANCE.enableMod)
                    .setDefaultValue(true)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Enabled") : Component.literal("Disabled"))
                    .setTooltip(Component.literal("If Chest Radar should be enabled."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.enableMod = newValue)
                    .build());

            features.addEntry(entryBuilder.startIntField(Component.literal("Scan Radius"), ModConfig.INSTANCE.scanRadius)
                    .setDefaultValue(24)
                    .setMin(2)
                    .setMax(64)
                    .setTooltip(Component.literal("Radius of blocks the mod will search (Singleplayer or server-side only)."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.scanRadius = newValue)
                    .build());

            features.addEntry(entryBuilder.startIntField(Component.literal("Scan Cooldown"), ModConfig.INSTANCE.scanCooldown)
                    .setDefaultValue(10)
                    .setMin(0)
                    .setMax(20)
                    .setTooltip(Component.literal("Number in ticks to wait bettween scans. Lower numbers are more responsive but use more processing power. (Singleplayer or server-side only)"))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.scanCooldown = newValue)
                    .build());

            var doItemDeltas = entryBuilder.startBooleanToggle(Component.literal("Item Deltas"), ModConfig.INSTANCE.doItemDelta)
                    .setDefaultValue(true)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Enabled") : Component.literal("Disabled"))
                    .setTooltip(Component.literal("When enabled, text showing the amount of items drained or pulled per second will be displayed."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.doItemDelta = newValue)
                    .build();

            features.addEntry(doItemDeltas);

            features.addEntry(entryBuilder.startBooleanToggle(Component.literal("Smooth Item Deltas"), ModConfig.INSTANCE.smoothItemDelta)
                    .setDefaultValue(true)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Enabled") : Component.literal("Disabled"))
                    .setTooltip(Component.literal("Recommended. When on, the item delta values are more stable but take longer to reach that stability. When off values may oscillate but reaches stability faster"))
                    .setRequirement(doItemDeltas::getValue)
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.smoothItemDelta = newValue)
                    .build());

            ConfigCategory rendering = builder.getOrCreateCategory(Component.literal("Rendering"));

            rendering.setDescription(new FormattedText[]{FormattedText.of("Rendering of outlines and text")});

            var renderOutlines = entryBuilder.startBooleanToggle(Component.literal("Render Outlines"), ModConfig.INSTANCE.renderOutlines)
                    .setDefaultValue(true)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Yes") : Component.literal("No"))
                    .setTooltip(Component.literal("If outlines should be rendered."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.renderOutlines = newValue)
                    .build();

            rendering.addEntry(entryBuilder.startBooleanToggle(Component.literal("Text Location"), ModConfig.INSTANCE.textLocation)
                    .setDefaultValue(true)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Center") : Component.literal("Above"))
                    .setTooltip(Component.literal("'Center' renders text in the center of blocks. 'Above' renders text above blocks"))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.textLocation = newValue)
                    .build());

            rendering.addEntry(renderOutlines);

            rendering.addEntry(entryBuilder.startBooleanToggle(Component.literal("Slim Outlines"), ModConfig.INSTANCE.slimOutlines)
                    .setDefaultValue(false)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Yes") : Component.literal("No"))
                    .setTooltip(Component.literal("Outlines becomes smaller. Chest outlines look better with this enabled, but outlines on barrels and shulker boxes are not visible."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.slimOutlines = newValue)
                    .setRequirement(renderOutlines::getValue)
                    .build());

            rendering.addEntry(entryBuilder.startFloatField(Component.literal("Outline Thickness"), ModConfig.INSTANCE.outlineThickness)
                    .setDefaultValue(4)
                    .setMin(1)
                    .setMax(6)
                    .setTooltip(Component.literal("How thick outlines will be."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.outlineThickness = newValue)
                    .setRequirement(renderOutlines::getValue)
                    .build());

            var colors = entryBuilder.startSubCategory(Component.literal("Color Gradient"))
                    .setExpanded(false)
                    .setTooltip(Component.literal("Value of items in a container for different colors."));

            colors.add(entryBuilder.startIntField(Component.literal("Red Amount"), ModConfig.INSTANCE.redAmount)
                    .setDefaultValue(1)
                    .setMin(1)
                    .setMax(1728)
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.redAmount = newValue)
                    .build());

            colors.add(entryBuilder.startIntField(Component.literal("Yellow Amount"), ModConfig.INSTANCE.yellowAmount)
                    .setDefaultValue(32)
                    .setMin(1)
                    .setMax(1728)
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.yellowAmount = newValue)
                    .build());

            colors.add(entryBuilder.startIntField(Component.literal("Green Amount"), ModConfig.INSTANCE.greenAmount)
                    .setDefaultValue(64)
                    .setMin(1)
                    .setMax(1728)
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.greenAmount = newValue)
                    .build());

            rendering.addEntry(colors.build());

            ConfigCategory controls = builder.getOrCreateCategory(Component.literal("Controls"));

            controls.setDescription(new FormattedText[]{FormattedText.of("Keybind behaviour")});

            controls.addEntry(entryBuilder.startBooleanToggle(Component.literal("Search Keybind Mode"), ModConfig.INSTANCE.toggleMode)
                    .setYesNoTextSupplier(bool -> bool ? Component.literal("Toggle") : Component.literal("Hold"))
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Hold or toggle keybind."))
                    .setSaveConsumer(newValue -> ModConfig.INSTANCE.toggleMode = newValue)
                    .build());

            builder.setSavingRunnable(ModConfig::save);

            return builder.build();
        };
    }
}