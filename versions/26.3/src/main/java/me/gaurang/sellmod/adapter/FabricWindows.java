package me.gaurang.sellmod.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import me.gaurang.sellmod.core.port.WindowPort;

/**
 * 26.3 screen/menu identity port (identical to 26.2). Tokens are the live screen and menu
 * instances; reference equality reproduces the original controller's
 * screen-vs-menu swap detection. Screen state moved from Minecraft to Gui
 * on 26.2+, so access goes through {@code Minecraft.gui}.
 */
public class FabricWindows implements WindowPort {
    @Override
    public boolean containerScreenOpen() {
        return Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>;
    }

    @Override
    public Object screenToken() {
        return Minecraft.getInstance().gui.screen();
    }

    @Override
    public Object screenMenuToken() {
        return Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen
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
