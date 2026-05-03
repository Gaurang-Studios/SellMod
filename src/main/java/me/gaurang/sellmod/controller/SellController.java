package me.gaurang.sellmod.controller;

import me.gaurang.sellmod.config.ModConfig;
import me.gaurang.sellmod.state.SellState;
import me.gaurang.sellmod.analytics.SellAnalytics;
import me.gaurang.sellmod.SellModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

import java.util.Random;

public class SellController {

    private final ModConfig config = ModConfig.INSTANCE;
    private final Random random = new Random();

    private SellState state = SellState.IDLE;

    private int waitTicks;
    private int cooldownTicks;
    private int actionDelayTicks;

    private AbstractContainerMenu handler;

    private boolean movedThisCycle = false;
    private boolean guiWasOpened = false;

    private int stallTicks = 0;
    private static final int STALL_THRESHOLD = 40;

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

    public void onClientTick(Minecraft client) {
        if (!config.enabled || client.player == null) return;

        if (guiWasOpened
                && state != SellState.IDLE
                && !(client.screen instanceof AbstractContainerScreen<?>)) {

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

    private void sendCommand(Minecraft client) {
        String cmd = config.sellCommand;
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        SellAnalytics.recordUsage(client, SellModClient.MOD_VERSION);

        client.player.connection.sendCommand(cmd);
        waitTicks = 40;
        state = SellState.WAIT_FOR_GUI;
    }

    private void waitForGui(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen<?> screen) {
            handler = screen.getMenu();
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

    private void dumpInventory(Minecraft client) {
        if (actionDelayTicks-- > 0) return;
        if (!(client.screen instanceof AbstractContainerScreen<?>)) return;
        if (handler == null) return;

        MultiPlayerGameMode im = client.gameMode;
        if (im == null) return;

        int burst = Math.max(1, config.transferBurst);
        boolean useShift = config.transferMode == ModConfig.TransferMode.SHIFT;

        boolean movedThisTick = false;

        while (burst-- > 0) {

            // SHIFT
            if (useShift) {
                boolean moved = false;

                for (Slot slot : handler.slots) {
                    if (!(slot.container instanceof Inventory)) continue;
                    if (!slot.hasItem()) continue;

                    im.handleContainerInput(
                            handler.containerId,
                            slot.index,
                            0,
                            ContainerInput.QUICK_MOVE,
                            client.player
                    );

                    moved = true;
                    movedThisTick = true;
                    movedThisCycle = true;
                    break;
                }

                if (!moved) break;
                continue;
            }

            // PICKUP
            if (!handler.getCarried().isEmpty()) {
                actionDelayTicks = 2;
                return;
            }

            boolean moved = false;

            for (Slot from : handler.slots) {
                if (!(from.container instanceof Inventory)) continue;
                if (!from.hasItem()) continue;

                // pick up
                im.handleContainerInput(
                        handler.containerId,
                        from.index,
                        0,
                        ContainerInput.PICKUP,
                        client.player
                );

                for (Slot to : handler.slots) {
                    if (to.container instanceof Inventory) continue;
                    if (!to.mayPlace(handler.getCarried())) continue;

                    // place
                    im.handleContainerInput(
                            handler.containerId,
                            to.index,
                            0,
                            ContainerInput.PICKUP,
                            client.player
                    );

                    moved = true;
                    movedThisTick = true;
                    movedThisCycle = true;
                    break;
                }

                // revert if failed
                if (!moved && !handler.getCarried().isEmpty()) {
                    im.handleContainerInput(
                            handler.containerId,
                            from.index,
                            0,
                            ContainerInput.PICKUP,
                            client.player
                    );
                }

                break;
            }

            if (!moved) break;
        }

        boolean playerHasItems = false;

        for (Slot slot : handler.slots) {
            if (slot.container instanceof Inventory && slot.hasItem()) {
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
            if (!(slot.container instanceof Inventory) && !slot.hasItem()) {
                return true;
            }
        }

        return false;
    }

    private void closeGui(Minecraft client) {
        client.player.closeContainer();

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
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().getToastManager().addToast(
                        new SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.literal(title),
                                Component.literal(msg)
                        )
                )
        );
    }
}