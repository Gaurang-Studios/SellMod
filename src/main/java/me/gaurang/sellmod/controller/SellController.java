package me.gaurang.sellmod.controller;

import me.gaurang.sellmod.config.ModConfig;
import me.gaurang.sellmod.state.SellState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.Random;

public class SellController {

    private final ModConfig config = ModConfig.INSTANCE;
    private final Random random = new Random();

    private SellState state = SellState.IDLE;

    private int waitTicks;
    private int cooldownTicks;
    private int actionDelayTicks;

    private ScreenHandler handler;

    private boolean movedThisCycle = false;
    private boolean guiWasOpened = false; // critical fix

    /* =========================
       Public API
       ========================= */

    public void enable() {
        state = SellState.SEND_COMMAND;
        guiWasOpened = false;
        showToast("SellMod enabled", "Auto-selling started");
    }

    public void disable() {
        state = SellState.IDLE;
        handler = null;
        guiWasOpened = false;
        showToast("SellMod disabled", "Auto-selling stopped");
    }

    public boolean isEnabled() {
        return state != SellState.IDLE;
    }

    /* =========================
       Tick entry
       ========================= */

    public void onClientTick(MinecraftClient client) {
        if (!config.enabled || client.player == null) return;

        // Manual close detection ONLY after GUI was opened
        if (guiWasOpened
                && state != SellState.IDLE
                && !(client.currentScreen instanceof HandledScreen<?>)) {

            handler = null;
            guiWasOpened = false;
            state = SellState.COOLDOWN;
            cooldownTicks = 40;

            showToast("SellMod paused", "GUI closed manually");
            return;
        }

        switch (state) {
            case SEND_COMMAND -> sendCommand(client);
            case WAIT_FOR_GUI -> waitForGui(client);
            case MOVE_ITEMS -> dumpInventory(client);
            case CLOSE_GUI -> closeGui(client);
            case COOLDOWN -> cooldown();
        }
    }

    /* =========================
       States
       ========================= */

    private void sendCommand(MinecraftClient client) {
        String cmd = config.sellCommand;
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        client.player.networkHandler.sendChatCommand(cmd);

        waitTicks = 40; // ~2 seconds
        state = SellState.WAIT_FOR_GUI;
    }

    private void waitForGui(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen<?> screen) {
            handler = screen.getScreenHandler();
            movedThisCycle = false;
            guiWasOpened = true;
            actionDelayTicks = 5;

            showToast("Sell GUI opened", "Dumping inventory…");
            state = SellState.MOVE_ITEMS;
            return;
        }

        if (--waitTicks <= 0) {
            state = SellState.SEND_COMMAND;
        }
    }

    private void dumpInventory(MinecraftClient client) {
        if (actionDelayTicks-- > 0) return;
        if (!(client.currentScreen instanceof HandledScreen<?>)) return;
        if (handler == null) return;

        ClientPlayerInteractionManager im = client.interactionManager;
        if (im == null) return;

        // Cursor must be empty before starting
        if (!handler.getCursorStack().isEmpty()) {
            for (Slot slot : handler.slots) {
                if (slot.inventory instanceof PlayerInventory
                        && slot.canInsert(handler.getCursorStack())) {
                    im.clickSlot(handler.syncId, slot.id, 0,
                            SlotActionType.PICKUP, client.player);
                    return;
                }
            }
            return;
        }

        // Pick from player inventory
        for (Slot from : handler.slots) {
            if (!(from.inventory instanceof PlayerInventory)) continue;
            if (from.getStack().isEmpty()) continue;

            im.clickSlot(handler.syncId, from.id, 0,
                    SlotActionType.PICKUP, client.player);

            // Try placing into container
            for (Slot to : handler.slots) {
                if (to.inventory instanceof PlayerInventory) continue;
                if (!to.canInsert(handler.getCursorStack())) continue;

                im.clickSlot(handler.syncId, to.id, 0,
                        SlotActionType.PICKUP, client.player);

                if (handler.getCursorStack().isEmpty()) {
                    movedThisCycle = true;
                    actionDelayTicks = rollItemDelay();
                    return;
                }
            }

            // Could not place → put back
            im.clickSlot(handler.syncId, from.id, 0,
                    SlotActionType.PICKUP, client.player);

            // Container full
            state = SellState.CLOSE_GUI;
            return;
        }

        // Nothing left to move
        state = SellState.CLOSE_GUI;
    }

    private void closeGui(MinecraftClient client) {
        if (movedThisCycle) {
            client.player.closeHandledScreen();
            showToast("Items sold", "Waiting for next cycle");
        }

        handler = null;
        guiWasOpened = false;

        cooldownTicks = rollCooldown();
        state = SellState.COOLDOWN;
    }

    private void cooldown() {
        if (--cooldownTicks <= 0) {
            state = SellState.SEND_COMMAND;
        }
    }

    /* =========================
       Utils
       ========================= */

    private int rollItemDelay() {
        int base = config.itemMoveDelayTicks;
    
        if (!config.randomizeItemDelay) {
            return base;
        }
    
        return Math.max(1, base + random.nextInt(5) - 2); // ±2 ticks
    }
    
    private int rollCooldown() {
        int base = config.baseDelaySeconds * 20;
    
        if (!config.randomizeDelay) {
            return base;
        }
    
        return base + random.nextInt(41) - 20; // ±2 seconds
    }

    private void showToast(String title, String msg) {
        MinecraftClient.getInstance().execute(() ->
            MinecraftClient.getInstance().getToastManager().add(
                new SystemToast(
                    SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal(title),
                    Text.literal(msg)
                )
            )
        );
    }
}