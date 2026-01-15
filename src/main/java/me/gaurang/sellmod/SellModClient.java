package me.gaurang.sellmod;

import me.gaurang.sellmod.config.ModConfig;
import me.gaurang.sellmod.controller.SellController;
import me.gaurang.sellmod.input.Keybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SellModClient implements ClientModInitializer {

    public static final SellController SELL_CONTROLLER = new SellController();

    @Override
    public void onInitializeClient() {

        Keybinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Sync controller with config
            if (ModConfig.INSTANCE.enabled && !SELL_CONTROLLER.isEnabled()) {
                SELL_CONTROLLER.enable();
            } else if (!ModConfig.INSTANCE.enabled && SELL_CONTROLLER.isEnabled()) {
                SELL_CONTROLLER.disable();
            }

            SELL_CONTROLLER.onClientTick(client);
            Keybinds.handle();
        });
    }
}