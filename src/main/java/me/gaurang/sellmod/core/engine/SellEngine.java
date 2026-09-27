package me.gaurang.sellmod.core.engine;

import me.gaurang.sellmod.core.SellState;
import me.gaurang.sellmod.core.analytics.AnalyticsEngine;
import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.detect.ButtonScorer;
import me.gaurang.sellmod.core.port.ClickKind;
import me.gaurang.sellmod.core.port.CommandPort;
import me.gaurang.sellmod.core.port.FeedbackPort;
import me.gaurang.sellmod.core.port.InventoryPort;
import me.gaurang.sellmod.core.port.MenuSlot;
import me.gaurang.sellmod.core.port.MenuSnapshot;
import me.gaurang.sellmod.core.port.WindowPort;

import java.util.Random;

/**
 * Version-independent sell state machine. This is the former 26.x-only
 * {@code SellController} mechanically translated onto port interfaces; every
 * decision point, timing constant, reset set and branch order is unchanged.
 */
public class SellEngine {
    private static final int GUI_WAIT_TICKS = 40;
    private static final int MAX_GUI_ATTEMPTS = 3;
    private static final int STALL_THRESHOLD = 40;

    private final ModConfig config = ModConfig.INSTANCE;
    private final Random random = new Random();

    private final CommandPort commands;
    private final WindowPort windows;
    private final InventoryPort inventory;
    private final FeedbackPort feedback;
    private final AnalyticsEngine analytics;

    private SellState state = SellState.IDLE;
    private int waitTicks;
    private int cooldownTicks;
    private int actionDelayTicks;
    private int buttonClickDelayTicks;
    private int buttonProcessingTicks;
    private int pendingButtonSlot = -1;
    private int stallTicks = 0;
    private int guiAttempts = 0;
    private boolean movedThisCycle = false;
    private boolean guiWasOpened = false;
    private int lastCooldownTicks = -1;

    // Opaque identity tokens mirroring the original handler/screen fields.
    private Object menuBeforeCommand;
    private Object activeMenu;
    private boolean menuAttached = false;
    private Object activeScreen;

    public SellEngine(CommandPort commands, WindowPort windows, InventoryPort inventory,
                      FeedbackPort feedback, AnalyticsEngine analytics) {
        this.commands = commands;
        this.windows = windows;
        this.inventory = inventory;
        this.feedback = feedback;
        this.analytics = analytics;
    }

    public void enable() {
        state = SellState.SEND_COMMAND;
        guiWasOpened = false;
        menuBeforeCommand = null;
        activeMenu = null;
        menuAttached = false;
        activeScreen = null;
        movedThisCycle = false;
        stallTicks = 0;
        guiAttempts = 0;
        actionDelayTicks = 0;
        buttonClickDelayTicks = 0;
        buttonProcessingTicks = 0;
        pendingButtonSlot = -1;
        showToast("SellMod enabled", "Auto-selling started");
    }

    public void disable() {
        state = SellState.IDLE;
        menuBeforeCommand = null;
        activeMenu = null;
        menuAttached = false;
        activeScreen = null;
        guiWasOpened = false;
        movedThisCycle = false;
        stallTicks = 0;
        guiAttempts = 0;
        actionDelayTicks = 0;
        buttonClickDelayTicks = 0;
        buttonProcessingTicks = 0;
        pendingButtonSlot = -1;
        lastCooldownTicks = -1;
        showToast("SellMod disabled", "Auto-selling stopped");
    }

    public boolean isEnabled() {
        return state != SellState.IDLE;
    }

    public void tick() {
        if (!config.enabled || !commands.playerAvailable()) return;

        // Preserve the original lifecycle: if the player/server closes the container,
        // the automation continues with the normal cooldown/reopen cycle.
        if (guiWasOpened
                && state != SellState.IDLE
                && !windows.containerScreenOpen()) {
            activeMenu = null;
            menuAttached = false;
            activeScreen = null;
            guiWasOpened = false;
            state = SellState.COOLDOWN;
            cooldownTicks = rollCooldown();
            return;
        }

        switch (state) {
            case SEND_COMMAND -> sendCommand();
            case WAIT_FOR_GUI -> waitForGui();
            case MOVE_ITEMS -> moveItems();
            case CLOSE_GUI -> closeGui();
            case CLICK_SELL_BUTTON -> clickSellButton();
            case WAIT_FOR_BUTTON -> waitForButton();
            case COOLDOWN -> cooldown();
            case IDLE -> { }
        }
    }

    private void sendCommand() {
        String cmd = config.sellCommand == null ? "" : config.sellCommand.trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.isBlank()) {
            disableForFatalError("SellMod disabled", "Sell command is empty");
            return;
        }

        menuBeforeCommand = commands.captureActiveMenuToken();
        activeMenu = null;
        menuAttached = false;
        activeScreen = null;
        guiWasOpened = false;
        movedThisCycle = false;
        stallTicks = 0;
        actionDelayTicks = 0;
        buttonClickDelayTicks = 0;
        buttonProcessingTicks = 0;
        guiAttempts++;

        analytics.onSellCommand(config);
        commands.sendChatCommand(cmd);
        waitTicks = GUI_WAIT_TICKS;
        state = SellState.WAIT_FOR_GUI;
    }

    private void waitForGui() {
        if (windows.containerScreenOpen()) {
            Object screenMenu = windows.screenMenuToken();
            if (screenMenu != menuBeforeCommand && windows.activeMenuToken() == screenMenu) {
                activeMenu = screenMenu;
                menuAttached = true;
                activeScreen = windows.screenToken();
                guiWasOpened = true;
                movedThisCycle = false;
                stallTicks = 0;
                actionDelayTicks = 0;
                buttonClickDelayTicks = 0;
                buttonProcessingTicks = 0;
                pendingButtonSlot = -1;
                guiAttempts = 0;
                feedback.toast("Sell GUI opened", config.sellThroughButton
                        ? "Filling inventory before pressing Sell…"
                        : "Dumping inventory…");
                state = SellState.MOVE_ITEMS;
                return;
            }
        }

        if (--waitTicks > 0) return;

        if (guiAttempts >= MAX_GUI_ATTEMPTS) {
            disableForFatalError("SellMod disabled", "Could not open sell GUI");
            return;
        }

        // Backoff between failed GUI-open attempts: 2s, 4s, then a final attempt.
        cooldownTicks = Math.min(160, GUI_WAIT_TICKS * (1 << Math.min(2, guiAttempts - 1)));
        state = SellState.COOLDOWN;
    }

    private void moveItems() {
        if (!windows.containerScreenOpen()) {
            abandonToCooldown();
            return;
        }

        // The screen token identifies the logical GUI for this cycle. A server may
        // replace its menu while keeping the same screen; that replacement is allowed.
        // A completely different screen (e.g. a chest opened by the player) is ignored.
        Object screenToken = windows.screenToken();
        if (screenToken != activeScreen) {
            abandonToCooldown();
            return;
        }
        adoptCurrentMenu();
        if (!menuAttached) return;

        if (actionDelayTicks-- > 0) return;
        if (!commands.interactionsAvailable()) return;

        int burst = Math.max(1, config.transferBurst);
        boolean useShift = config.transferMode == ModConfig.TransferMode.SHIFT;
        boolean movedThisTick = false;

        while (burst-- > 0) {
            MenuSnapshot snapshot = inventory.snapshot();
            MenuSlot from = findNextSellableSlot(snapshot);
            if (from == null) break;

            if (useShift) {
                inventory.click(from.index(), ClickKind.QUICK_MOVE);
                analytics.recordItemsTransferred(from.itemCount());
                movedThisTick = true;
                movedThisCycle = true;
                continue;
            }

            if (snapshot.carriedCount() > 0) {
                actionDelayTicks = 2;
                // A permanently stuck cursor must not bypass stall detection:
                // count it and close the GUI (which returns the cursor item to
                // the inventory) once the threshold is hit instead of looping.
                if (++stallTicks >= STALL_THRESHOLD) {
                    stallTicks = 0;
                    state = SellState.CLOSE_GUI;
                }
                return;
            }

            int destination = -1;
            for (MenuSlot to : snapshot.slots()) {
                if (to.playerOwned() || to.hasItem()) continue;
                if (!inventory.mayPlaceOn(to.index(), from.index())) continue;
                destination = to.index();
                break;
            }

            if (destination >= 0) {
                inventory.click(from.index(), ClickKind.PICKUP);
                inventory.click(destination, ClickKind.PICKUP);
                analytics.recordItemsTransferred(from.itemCount());
                movedThisTick = true;
                movedThisCycle = true;
            } else {
                if (snapshot.carriedCount() > 0) {
                    inventory.click(from.index(), ClickKind.PICKUP);
                }
                break;
            }
        }

        // Boundary checks read the live menu after prediction-mutating clicks.
        MenuSnapshot current = inventory.snapshot();
        boolean playerHasItems = hasSellablePlayerItem(current);
        boolean guiHasSpace = guiHasEmptySlot(current);

        // The original transfer boundary is intentionally simple: once the GUI is full
        // or there are no more player items to move, the current sell action fires.
        if (!guiHasSpace || !playerHasItems) {
            finishTransferAction(current);
            return;
        }

        if (movedThisTick) {
            stallTicks = 0;
        } else {
            stallTicks++;
            if (stallTicks >= STALL_THRESHOLD) {
                stallTicks = 0;
                finishTransferAction(inventory.snapshot());
                return;
            }
        }

        scheduleNextItemDelay();
    }

    private void finishTransferAction(MenuSnapshot snapshot) {
        boolean throughButton = config.sellThroughButton && hasSellableGuiItem(snapshot);
        analytics.recordSellSubmitted(guiItemCount(snapshot), throughButton);
        if (throughButton) {
            buttonClickDelayTicks = rollButtonDelay();
            state = SellState.CLICK_SELL_BUTTON;
            return;
        }
        state = SellState.CLOSE_GUI;
    }

    private MenuSlot findNextSellableSlot(MenuSnapshot snapshot) {
        for (MenuSlot slot : snapshot.slots()) {
            if (slot.playerOwned() && slot.hasItem() && !isProtectedSlot(slot)) {
                return slot;
            }
        }
        return null;
    }

    private boolean guiHasEmptySlot(MenuSnapshot snapshot) {
        for (MenuSlot slot : snapshot.slots()) {
            if (!slot.playerOwned() && !slot.hasItem()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSellableGuiItem(MenuSnapshot snapshot) {
        for (MenuSlot slot : snapshot.slots()) {
            if (!slot.playerOwned() && slot.hasItem()) {
                return true;
            }
        }
        return false;
    }

    private int guiItemCount(MenuSnapshot snapshot) {
        int total = 0;
        for (MenuSlot slot : snapshot.slots()) {
            if (!slot.playerOwned() && slot.hasItem()) {
                total += slot.itemCount();
            }
        }
        return total;
    }

    private boolean hasSellablePlayerItem(MenuSnapshot snapshot) {
        for (MenuSlot slot : snapshot.slots()) {
            if (slot.playerOwned() && slot.hasItem() && !isProtectedSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtectedSlot(MenuSlot slot) {
        return ModConfig.isProtected(config.protectedSlots, slot.containerSlot());
    }

    private void scheduleNextItemDelay() {
        int base = Math.max(1, config.itemMoveDelayTicks);
        if (!config.randomizeItemDelay) {
            actionDelayTicks = base;
            return;
        }
        // Keep the original config's intended ±2 tick item-delay randomization.
        actionDelayTicks = Math.max(1, base + random.nextInt(5) - 2);
    }

    private void clickSellButton() {
        if (!windows.containerScreenOpen() || windows.screenToken() != activeScreen) {
            cooldownTicks = rollCooldown();
            state = SellState.COOLDOWN;
            activeMenu = null;
            menuAttached = false;
            activeScreen = null;
            guiWasOpened = false;
            return;
        }
        adoptCurrentMenu();
        if (!menuAttached) return;

        if (buttonClickDelayTicks-- > 0) return;

        // A button interaction must begin with an empty cursor. If a previous
        // interaction left an item there, don't stack another click on top of it.
        if (inventory.snapshot().carriedCount() > 0) {
            disableForFatalError("SellMod disabled", "Sell button left an item on the cursor");
            return;
        }

        MenuSlot button = ButtonScorer.findBest(inventory.snapshot(), config);
        if (button == null) {
            disableForFatalError("SellMod disabled", "Could not detect a Sell button");
            return;
        }

        pendingButtonSlot = button.index();
        inventory.click(button.index(), ClickKind.PICKUP);
        movedThisCycle = false;
        buttonProcessingTicks = Math.max(1, config.buttonProcessingDelayTicks);
        state = SellState.WAIT_FOR_BUTTON;
    }

    private void waitForButton() {
        if (!windows.containerScreenOpen()) {
            abandonToCooldown();
            return;
        }
        if (windows.screenToken() != activeScreen) {
            // The server replaced the whole screen. Treat that as the normal end
            // of this GUI cycle; do not blindly adopt an unrelated container.
            abandonToCooldown();
            return;
        }
        adoptCurrentMenu();
        if (!menuAttached) return;

        // If the click accidentally picked the button item up instead of activating
        // it, try one compensating click on the same slot. If the cursor is still
        // non-empty afterwards, fail closed rather than trapping the item forever.
        if (inventory.snapshot().carriedCount() > 0 && pendingButtonSlot >= 0) {
            if (commands.interactionsAvailable() && buttonProcessingTicks <= 1) {
                inventory.click(pendingButtonSlot, ClickKind.PICKUP);
                buttonProcessingTicks = 1;
                pendingButtonSlot = -1;
                return;
            }
        }

        if (--buttonProcessingTicks > 0) return;

        if (inventory.snapshot().carriedCount() > 0) {
            disableForFatalError("SellMod disabled", "Sell button left an item on the cursor");
            return;
        }

        // Success requires no GUI-content, money, packet, or inventory
        // confirmation: continuous farms may leave the sell input looking
        // identical after a sale. Cursor safety above is the only gate; the
        // cycle always continues.
        pendingButtonSlot = -1;
        state = SellState.MOVE_ITEMS;
        actionDelayTicks = Math.max(1, config.itemMoveDelayTicks);
        stallTicks = 0;
    }

    private void closeGui() {
        windows.closeIfActive(activeMenu);

        if (movedThisCycle) {
            feedback.toast("Items sold", "Waiting for next cycle");
        }

        activeMenu = null;
        menuAttached = false;
        menuBeforeCommand = null;
        activeScreen = null;
        guiWasOpened = false;
        movedThisCycle = false;
        stallTicks = 0;
        guiAttempts = 0;
        pendingButtonSlot = -1;
        cooldownTicks = rollCooldown();
        state = SellState.COOLDOWN;
    }

    private void cooldown() {
        if (--cooldownTicks <= 0) {
            state = SellState.SEND_COMMAND;
        }
    }

    /** Adopts a server-side menu replacement within the same screen, as before. */
    private void adoptCurrentMenu() {
        Object screenMenu = windows.screenMenuToken();
        if (screenMenu != activeMenu || windows.activeMenuToken() != activeMenu) {
            activeMenu = screenMenu;
        }
    }

    private void abandonToCooldown() {
        activeMenu = null;
        menuAttached = false;
        activeScreen = null;
        guiWasOpened = false;
        cooldownTicks = rollCooldown();
        state = SellState.COOLDOWN;
    }

    private int rollCooldown() {
        int base = Math.max(20, config.baseDelaySeconds * 20);
        if (!config.randomizeDelay) {
            lastCooldownTicks = base;
            return base;
        }

        // Preserve the original ±20 tick range while avoiding a predictable
        // repeated interval across consecutive cycles.
        int next = base;
        for (int attempt = 0; attempt < 6; attempt++) {
            next = Math.max(20, base + random.nextInt(41) - 20);
            if (next != lastCooldownTicks) break;
        }
        lastCooldownTicks = next;
        return next;
    }

    private int rollButtonDelay() {
        int base = Math.max(1, config.buttonClickDelayTicks);
        if (!config.randomizeButtonDelay) {
            return base;
        }
        return Math.max(1, base + random.nextInt(5) - 2);
    }

    private void disableForFatalError(String title, String message) {
        config.enabled = false;
        config.save();
        state = SellState.IDLE;
        menuBeforeCommand = null;
        activeMenu = null;
        menuAttached = false;
        activeScreen = null;
        guiWasOpened = false;
        movedThisCycle = false;
        stallTicks = 0;
        guiAttempts = 0;
        actionDelayTicks = 0;
        buttonClickDelayTicks = 0;
        buttonProcessingTicks = 0;
        pendingButtonSlot = -1;
        lastCooldownTicks = -1;
        feedback.toast(title, message);
    }

    private void showToast(String title, String msg) {
        feedback.toast(title, msg);
    }
}
