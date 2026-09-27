package me.gaurang.sellmod.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.inventory.ClickType;
import me.gaurang.sellmod.core.port.ClickKind;
import me.gaurang.sellmod.core.port.CommandPort;

/** 1.21.11 command/connection port backed by the vanilla client. */
public class FabricCommands implements CommandPort {
    @Override
    public boolean playerAvailable() {
        return Minecraft.getInstance().player != null;
    }

    @Override
    public boolean interactionsAvailable() {
        return Minecraft.getInstance().gameMode != null;
    }

    @Override
    public void sendChatCommand(String commandWithoutSlash) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.connection.sendCommand(commandWithoutSlash);
    }

    @Override
    public Object captureActiveMenuToken() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : player.containerMenu;
    }

    /** Kept for parity with the 26.x adapter's explicit gameMode reads. */
    static MultiPlayerGameMode gameMode() {
        return Minecraft.getInstance().gameMode;
    }

    static boolean click(int slotIndex, ClickKind kind) {
        var client = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = client.gameMode;
        var player = client.player;
        var menu = player == null ? null : player.containerMenu;
        if (gameMode == null || player == null || menu == null) {
            return false;
        }
        gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0,
                kind == ClickKind.PICKUP ? ClickType.PICKUP : ClickType.QUICK_MOVE,
                player);
        return true;
    }
}
