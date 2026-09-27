package me.gaurang.sellmod.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import me.gaurang.sellmod.core.port.WindowPort;

/**
 * 26.x screen/menu identity port. Tokens are the live screen and menu
 * instances; reference equality reproduces the original controller's
 * screen-vs-menu swap detection.
 */
public class FabricWindows implements WindowPort {
    @Override
    public boolean containerScreenOpen() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;
    }

    @Override
    public Object screenToken() {
        return Minecraft.getInstance().screen;
    }

    @Override
    public Object screenMenuToken() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                ? screen.getMenu()
                : null;
    }

    @Override
    public Object activeMenuToken() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : player.containerMenu;
    }

    @Override
    public void closeIfActive(Object menuToken) {
        var client = Minecraft.getInstance();
        if (client.player != null && client.player.containerMenu == menuToken) {
            client.player.closeContainer();
        }
    }
}
