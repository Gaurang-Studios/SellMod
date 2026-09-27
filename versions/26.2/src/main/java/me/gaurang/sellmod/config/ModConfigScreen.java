package me.gaurang.sellmod.config;

import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.gui.ButtonEntry;
import me.gaurang.sellmod.gui.InventoryPreview;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public class ModConfigScreen {
    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.INSTANCE;
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("SellMod Config"))
                .setSavingRunnable(config::save);
        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(entry.startTextDescription(Component.literal(
                "SellMod automatically transfers your inventory into the server-provided sell GUI.\n" +
                        "Choose how the current GUI should trigger the sale.\n\n" +
                        "IMPORTANT:\n" +
                        "- Keep tools, keys, and important items out of your inventory.\n" +
                        "- The mod does NOT select specific sellable items.")).build());
        general.addEntry(entry.startBooleanToggle(Component.literal("Enabled"), config.enabled)
                .setDefaultValue(false)
                .setSaveConsumer(val -> config.enabled = val)
                .build());
        var sellThroughButtonEntry = entry.startBooleanToggle(Component.literal("Sell through Button"), config.sellThroughButton)
                .setDefaultValue(false)
                .setTooltip(Component.literal(
                        "OFF: Use the existing Sell on Close behavior.\n" +
                        "ON: Fill the GUI and press its detected Sell button instead of closing it."))
                .setSaveConsumer(val -> config.sellThroughButton = val)
                .build();
        general.addEntry(entry.startStrField(Component.literal("Sell Command"), config.sellCommand)
                .setDefaultValue("/sellgui")
                .setSaveConsumer(val -> config.sellCommand = val)
                .build());
        general.addEntry(entry.startEnumSelector(Component.literal("Transfer Mode"), ModConfig.TransferMode.class, config.transferMode)
                .setTooltip(Component.literal("PICKUP: Uses cursor pickup logic.\nSHIFT: Uses shift-click / quick-move logic.\n"))
                .setDefaultValue(ModConfig.TransferMode.SHIFT)
                .setSaveConsumer(val -> config.transferMode = val)
                .build());
        general.addEntry(entry.startIntField(Component.literal("Base Delay (seconds)"), config.baseDelaySeconds)
                .setDefaultValue(5)
                .setMin(1)
                .setSaveConsumer(val -> config.baseDelaySeconds = val)
                .build());
        general.addEntry(entry.startTextDescription(Component.literal(
                "Used between sell cycles when the GUI is closed.\n" +
                        "Button mode normally stays inside the same GUI, but this delay still applies if that GUI closes.")).build());
        general.addEntry(entry.startIntSlider(Component.literal("Item transfer speed (in TICKS)"), config.itemMoveDelayTicks, 1, 20)
                .setTooltip(Component.literal("Controls the delay between moving items into the sell GUI.\nLower = Faster but riskier.\nHigher = Slower but safer."))
                .setDefaultValue(4)
                .setTextGetter(val -> {
                    int ms = val * 50;
                    String safety = val <= 2 ? "Very Fast (Risky)"
                            : val <= 4 ? "Fast"
                            : val <= 7 ? "Normal"
                            : val <= 12 ? "Safe" : "Very Safe";
                    return Component.literal(val + " ticks  (~" + ms + " ms)  •  " + safety);
                })
                .setSaveConsumer(val -> config.itemMoveDelayTicks = val)
                .build());
        general.addEntry(entry.startIntSlider(Component.literal("Transfer Burst (stacks per tick)"), config.transferBurst, 1, 6)
                .setDefaultValue(3)
                .setTooltip(Component.literal("How many item stacks are moved per client tick.\nHigher = faster, but may trigger server limits.\n\n1 = Very Safe\n3 = Balanced (recommended)\n5+ = Fast (use carefully)"))
                .setSaveConsumer(val -> config.transferBurst = val)
                .build());
        general.addEntry(entry.startBooleanToggle(Component.literal("Randomize item transfer delay"), config.randomizeItemDelay)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Adds small per-transfer variation to item movement timing."))
                .setSaveConsumer(val -> config.randomizeItemDelay = val)
                .build());
        general.addEntry(entry.startBooleanToggle(Component.literal("Randomize sell cycle delay"), config.randomizeDelay)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Adds a small random offset to the delay between sell cycles."))
                .setSaveConsumer(val -> config.randomizeDelay = val)
                .build());
        general.addEntry(entry.startTextDescription(Component.literal(
                "Configuration version: " + config.configVersion + "\n" +
                        "Do not edit this value manually; it is used for future config migrations.")).build());

        ConfigCategory buttonCategory = builder.getOrCreateCategory(Component.literal("Sell Through Button"));
        buttonCategory.addEntry(entry.startTextDescription(Component.literal(
                "When enabled, SellMod fills the current sell GUI and presses its Sell button\n" +
                        "when the GUI is full or there are no more player items to transfer.\n" +
                        "If the button cannot be detected, Button mode fails instead of falling back to Sell on Close.")).build());
        buttonCategory.addEntry(sellThroughButtonEntry);

        var detectionModeEntry = entry.startEnumSelector(Component.literal("Button Detection"),
                        ModConfig.ButtonDetectionMode.class, config.buttonDetectionMode)
                .setDefaultValue(ModConfig.ButtonDetectionMode.HYBRID_AUTOMATIC)
                .setTooltip(Component.literal(
                        "HYBRID_AUTOMATIC: Uses item text, lore, material clues, language keywords, and GUI position.\n" +
                        "MANUAL_MATERIAL: Uses one exact configured item material, then uses GUI context to choose the best match."))
                .setSaveConsumer(val -> config.buttonDetectionMode = val)
                .setDisplayRequirement(Requirement.isTrue(sellThroughButtonEntry))
                .build();
        buttonCategory.addEntry(detectionModeEntry);

        Identifier selectedMaterial = resolveConfiguredMaterialId(config.sellButtonMaterial);
        List<Identifier> allItems = BuiltInRegistries.ITEM.keySet().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        var materialEntry = entry.startDropdownMenu(
                        Component.literal("Sell Button Material"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(selectedMaterial,
                                DropdownMenuBuilder.TopCellElementBuilder.ITEM_IDENTIFIER_FUNCTION),
                        DropdownMenuBuilder.CellCreatorBuilder.<Identifier>of(ModConfigScreen::materialDisplayName))
                .setDefaultValue(BuiltInRegistries.ITEM.getKey(Items.DYE.lime()))
                .setSelections(allItems)
                .setTooltip(Component.literal("Manual mode uses exactly one configured item material for the Sell button."))
                .setSaveConsumer(materialId -> config.sellButtonMaterial = materialId.toString())
                .setDisplayRequirement(Requirement.all(
                        Requirement.isTrue(sellThroughButtonEntry),
                        Requirement.isValue(detectionModeEntry, ModConfig.ButtonDetectionMode.MANUAL_MATERIAL)))
                .build();
        buttonCategory.addEntry(materialEntry);

        buttonCategory.addEntry(entry.startIntSlider(Component.literal("Button click delay (TICKS)"), config.buttonClickDelayTicks, 1, 20)
                .setDefaultValue(2)
                .setTooltip(Component.literal("Delay before pressing the detected Sell button. Randomization can add small variation."))
                .setSaveConsumer(val -> config.buttonClickDelayTicks = val)
                .setDisplayRequirement(Requirement.isTrue(sellThroughButtonEntry))
                .build());
        buttonCategory.addEntry(entry.startBooleanToggle(Component.literal("Randomize button click delay"), config.randomizeButtonDelay)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Adds the same small timing variation philosophy used by item transfers."))
                .setSaveConsumer(val -> config.randomizeButtonDelay = val)
                .setDisplayRequirement(Requirement.isTrue(sellThroughButtonEntry))
                .build());
        buttonCategory.addEntry(entry.startIntSlider(Component.literal("Button processing delay (TICKS)"), config.buttonProcessingDelayTicks, 1, 40)
                .setDefaultValue(4)
                .setTooltip(Component.literal("Wait after pressing Sell before scanning the GUI again. Useful for server-side GUI refreshes."))
                .setSaveConsumer(val -> config.buttonProcessingDelayTicks = val)
                .setDisplayRequirement(Requirement.isTrue(sellThroughButtonEntry))
                .build());

        ConfigCategory protectedSlotsCategory = builder.getOrCreateCategory(Component.literal("Protected Slots"));
        protectedSlotsCategory.addEntry(entry.startTextDescription(Component.literal(
                "Use the button below to open the visual editor.\n" +
                        "Click any inventory slot to toggle its protection (red tint = protected).\n" +
                        "Armor and offhand are always protected.\n" +
                        "Items in protected slots will never be moved into the sell GUI.")).build());
        final Screen[] configScreen = new Screen[1];
        protectedSlotsCategory.addEntry(ButtonEntry.create(
                Component.literal("Protected Slot Preview"),
                Button.builder(
                        Component.literal("Open Preview"),
                        button -> InventoryPreview.open(configScreen[0]))
                        .build()));

        ConfigCategory analytics = builder.getOrCreateCategory(Component.literal("Analytics"));
        analytics.addEntry(entry.startBooleanToggle(Component.literal("Anonymous analytics"), config.analyticsEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Sends anonymous usage events: a random per-launch session id, item/cycle counters, sell method, transfer method, and a feature settings snapshot.\nNo usernames, account IDs, server addresses, inventories, chat, coordinates, worlds, commands, or versions are collected.\nFully asynchronous and bounded: analytics failures never affect selling."))
                .setSaveConsumer(val -> config.analyticsEnabled = val)
                .build());

        configScreen[0] = builder.build();
        return configScreen[0];
    }

    private static Identifier resolveConfiguredMaterialId(String configured) {
        if (configured != null && !configured.isBlank()) {
            Identifier id = Identifier.tryParse(configured.trim());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                return id;
            }
        }
        return BuiltInRegistries.ITEM.getKey(Items.DYE.lime());
    }

    /**
     * Renders a dropdown cell as the item's display name without ever
     * constructing an ItemStack: Cloth's item-object cell creators build stacks
     * for every selection while the screen is being built, which crashes under
     * the 26.1 component-binding lifecycle ("Components not bound yet").
     */
    private static Component materialDisplayName(Identifier materialId) {
        return BuiltInRegistries.ITEM.get(materialId)
                .<Component>map(reference -> Component.translatable(reference.value().getDescriptionId()))
                .orElseGet(() -> Component.literal(materialId.toString()));
    }
}
