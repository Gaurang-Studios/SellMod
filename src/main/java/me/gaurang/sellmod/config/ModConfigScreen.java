package me.gaurang.sellmod.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModConfigScreen {

    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.INSTANCE;

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("SellMod Config"))
            .setSavingRunnable(() -> {});

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder entry = builder.entryBuilder();

        // Info text
        general.addEntry(
            entry.startTextDescription(
                Text.literal(
                    "SellMod automatically dumps your entire inventory\n" +
                    "into the sell GUI and closes it to sell items.\n\n" +
                    " IMPORTANT:\n" +
                    "- Keep tools, keys, and important items out of your inventory.\n" +
                    "- The mod does NOT select specific items.\n" +
                    "- Selling is based on GUI behavior (sell-on-close type of GUIs)."
                )
            ).build()
        );

        general.addEntry(
            entry.startBooleanToggle(Text.literal("Enabled"), config.enabled)
                .setDefaultValue(false)
                .setSaveConsumer(val -> config.enabled = val)
                .build()
        );

        general.addEntry(
            entry.startStrField(Text.literal("Sell Command"), config.sellCommand)
                .setDefaultValue("/sell")
                .setSaveConsumer(val -> config.sellCommand = val)
                .build()
        );

        general.addEntry(
            entry.startIntField(Text.literal("Base Delay (seconds)"), config.baseDelaySeconds)
                .setDefaultValue(5)
                .setMin(1)
                .setSaveConsumer(val -> config.baseDelaySeconds = val)
                .build()
        );

        general.addEntry(
            entry.startBooleanToggle(
                Text.literal(
                    "Require GUI Title Match\n" + 
                    "(You might not need this though)"
                ),
                config.requireTitleMatch
            )
                .setDefaultValue(false)
                .setSaveConsumer(val -> config.requireTitleMatch = val)
                .build()
        );

        general.addEntry(
            entry.startStrField(
                Text.literal("Expected GUI Title"),
                config.expectedTitle
            )
                .setDefaultValue("")
                .setSaveConsumer(val -> config.expectedTitle = val)
                .build()
        );

        return builder.build();
    }
}