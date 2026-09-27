package me.gaurang.sellmod;

import me.gaurang.sellmod.adapter.FabricCommands;
import me.gaurang.sellmod.adapter.FabricConfigScreens;
import me.gaurang.sellmod.adapter.FabricFeedback;
import me.gaurang.sellmod.adapter.FabricInventory;
import me.gaurang.sellmod.adapter.FabricTransport;
import me.gaurang.sellmod.adapter.FabricWindows;
import me.gaurang.sellmod.core.analytics.AnalyticsEngine;
import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.engine.SellEngine;
import me.gaurang.sellmod.input.Keybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class SellModClient implements ClientModInitializer {
    private static SellEngine engine;

    @Override
    public void onInitializeClient() {
        ModConfig.init(FabricLoader.getInstance().getConfigDir());

        AnalyticsEngine analytics = new AnalyticsEngine(new FabricTransport(),
                FabricLoader.getInstance().getConfigDir());
        analytics.start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> analytics.tryFlush());

        engine = new SellEngine(
                new FabricCommands(),
                new FabricWindows(),
                new FabricInventory(),
                new FabricFeedback(),
                analytics);

        Keybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfig.INSTANCE.enabled && !engine.isEnabled()) {
                engine.enable();
            } else if (!ModConfig.INSTANCE.enabled && engine.isEnabled()) {
                engine.disable();
            }
            engine.tick();
            Keybinds.handle(new FabricConfigScreens());
        });
    }

    public static SellEngine engine() {
        return engine;
    }
}
