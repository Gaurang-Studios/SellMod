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
            entry.startEnumSelector(
                    Text.literal("Transfer Mode"),
                    ModConfig.TransferMode.class,
                    config.transferMode
                )
                .setTooltip(Text.literal("""
                PICKUP: Uses cursor pickup logic.
                SHIFT: Uses shift-click only (best compatibility).
                """))
                .setDefaultValue(ModConfig.TransferMode.SHIFT)
                .setSaveConsumer(val -> config.transferMode = val)
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
            entry.startTextDescription(
                Text.literal(
                    "Adds a small random offset to the delay between sell cycles\n" +
                    "to avoid repetitive timing patterns"
                )
            ).build()
        );
        
        general.addEntry(
            entry.startIntSlider(
                    Text.literal("Item transfer speed (in TICKS)"),
                    config.itemMoveDelayTicks,
                    1,
                    20
            )
            .setTooltip(
                Text.literal(
                    "Controls the delay between moving items into the sell GUI.\n" +
                    "Lower = Faster but riskier.\n" +
                    "Higher = Slower but safer."
                )
            )
            .setDefaultValue(4)
            .setTextGetter(val -> {
                int ms = val * 50;
        
                String safety;
                if (val <= 2) safety = "Very Fast (Risky)";
                else if (val <= 4) safety = "Fast";
                else if (val <= 7) safety = "Normal";
                else if (val <= 12) safety = "Safe";
                else safety = "Very Safe";
        
                return Text.literal(
                        val + " ticks  (~" + ms + " ms)  •  " + safety
                );
            })
            .setSaveConsumer(val -> config.itemMoveDelayTicks = val)
            .build()
        );
        
        general.addEntry(
            entry.startIntSlider(
                    Text.literal("Transfer Burst (stacks per tick)"),
                    config.transferBurst,
                    1,
                    6
            )
            .setDefaultValue(3)
            .setTooltip(
                Text.literal(
                    "How many item stacks are moved per client tick.\n" +
                    "Higher = faster, but may trigger server limits.\n\n" +
                    "1  = Very Safe\n" +
                    "3  = Balanced (recommended)\n" +
                    "5+ = Fast (use carefully)"
                )
            )
            .setSaveConsumer(val -> config.transferBurst = val)
            .build()
        );
        
        general.addEntry(
            entry.startBooleanToggle(
                    Text.literal("Randomize item transfer delay"),
                    config.randomizeItemDelay
            )
            .setDefaultValue(true)
            .setSaveConsumer(val -> config.randomizeItemDelay = val)
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