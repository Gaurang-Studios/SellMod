package me.gaurang.sellmod.controller;

import me.gaurang.sellmod.config.ModConfig;
import me.gaurang.sellmod.state.SellState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerInventory;
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
    private boolean guiWasOpened = false;

    /* =========================
       Stall Detection
       ========================= */

    private int stallTicks = 0;
    private static final int STALL_THRESHOLD = 40;

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

    /* ========================= */

    public void onClientTick(MinecraftClient client) {
        if (!config.enabled || client.player == null) return;

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

    /* ========================= */

    private void sendCommand(MinecraftClient client) {
        String cmd = config.sellCommand;
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        client.player.networkHandler.sendChatCommand(cmd);
        waitTicks = 40;
        state = SellState.WAIT_FOR_GUI;
    }

    private void waitForGui(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen<?> screen) {
            handler = screen.getScreenHandler();
            movedThisCycle = false;
            guiWasOpened = true;
            stallTicks = 0;

            showToast("Sell GUI opened", "Dumping inventory…");
            state = SellState.MOVE_ITEMS;
            return;
        }

        if (--waitTicks <= 0) {
            state = SellState.SEND_COMMAND;
        }
    }

    /* =========================
       FIXED BURST LOGIC
       ========================= */

    private void dumpInventory(MinecraftClient client) {
        if (actionDelayTicks-- > 0) return;
        if (!(client.currentScreen instanceof HandledScreen<?>)) return;
        if (handler == null) return;

        ClientPlayerInteractionManager im = client.interactionManager;
        if (im == null) return;

        int burst = Math.max(1, config.transferBurst);
        boolean useShift = config.transferMode == ModConfig.TransferMode.SHIFT;

        boolean movedThisTick = false;

        while (burst-- > 0) {

            /* ================= SHIFT ================= */

            if (useShift) {
                boolean moved = false;

                for (Slot slot : handler.slots) {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if (!slot.hasStack()) continue;

                    im.clickSlot(handler.syncId, slot.id, 0,
                            SlotActionType.QUICK_MOVE, client.player);

                    moved = true;
                    movedThisTick = true;
                    movedThisCycle = true;
                    break;
                }

                if (!moved) break;

                continue; // <-- IMPORTANT (keeps burst alive)
            }

            /* ================= PICKUP ================= */

            if (!handler.getCursorStack().isEmpty()) {
                actionDelayTicks = 2;
                return;
            }

            boolean moved = false;

            for (Slot from : handler.slots) {
                if (!(from.inventory instanceof PlayerInventory)) continue;
                if (!from.hasStack()) continue;

                im.clickSlot(handler.syncId, from.id, 0,
                        SlotActionType.PICKUP, client.player);

                for (Slot to : handler.slots) {
                    if (to.inventory instanceof PlayerInventory) continue;
                    if (!to.canInsert(handler.getCursorStack())) continue;

                    im.clickSlot(handler.syncId, to.id, 0,
                            SlotActionType.PICKUP, client.player);

                    moved = true;
                    movedThisTick = true;
                    movedThisCycle = true;
                    break;
                }

                if (!moved && !handler.getCursorStack().isEmpty()) {
                    im.clickSlot(handler.syncId, from.id, 0,
                            SlotActionType.PICKUP, client.player);
                }

                break;
            }

            if (!moved) break;

            continue; // <-- IMPORTANT
        }

        /* ================= GUI FULL DETECTION ================= */

        boolean playerHasItems = false;

        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory && slot.hasStack()) {
                playerHasItems = true;
                break;
            }
        }

        if (!guiHasEmptySlot() && playerHasItems) {
            state = SellState.CLOSE_GUI;
            return;
        }

        if (movedThisTick) {
            stallTicks = 0;
        } else if (playerHasItems) {
            stallTicks++;
            if (stallTicks >= STALL_THRESHOLD) {
                stallTicks = 0;
                state = SellState.CLOSE_GUI;
                return;
            }
        } else {
            state = SellState.CLOSE_GUI;
            return;
        }

        actionDelayTicks = config.itemMoveDelayTicks;
    }

    private boolean guiHasEmptySlot() {
        if (handler == null) return false;

        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory) && !slot.hasStack()) {
                return true;
            }
        }

        return false;
    }

    private void closeGui(MinecraftClient client) {
        
        client.player.closeHandledScreen();
    
        if (movedThisCycle) {
            showToast("Items sold", "Waiting for next cycle");
        }
    
        handler = null;
        guiWasOpened = false;
        movedThisCycle = false;
    
        cooldownTicks = rollCooldown();
        state = SellState.COOLDOWN;
    }

    private void cooldown() {
        if (--cooldownTicks <= 0) {
            state = SellState.SEND_COMMAND;
        }
    }

    private int rollCooldown() {
        int base = config.baseDelaySeconds * 20;
        if (!config.randomizeDelay) return base;
        return base + random.nextInt(41) - 20;
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