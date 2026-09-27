package me.gaurang.sellmod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class InventoryPreview {
    private InventoryPreview() {
    }

    public static void open(Screen returnTo) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            SystemToast.addOrUpdate(client.getToastManager(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("SellMod Preview"),
                    Component.literal("Join a world first to use the protected-slot preview."));
            return;
        }
        client.setScreen(new PreviewScreen(returnTo));
    }
}
