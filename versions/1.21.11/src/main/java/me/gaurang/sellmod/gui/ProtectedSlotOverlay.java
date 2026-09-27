package me.gaurang.sellmod.gui;

import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 1.21.11 protected-slot overlay painter (classic GuiGraphics pipeline). */
public final class ProtectedSlotOverlay {
    private static final int SLOT_SIZE = 16;
    private static final int OVERLAY_TINT = 822033200;

    private ProtectedSlotOverlay() {
    }

    public static void render(AbstractContainerScreen<?> containerScreen, GuiGraphics graphics) {
        boolean[] protectedSlots = ModConfig.INSTANCE.protectedSlots;
        if (protectedSlots == null) {
            return;
        }
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) containerScreen;
        int left = accessor.sellmod$getLeftPos();
        int top = accessor.sellmod$getTopPos();
        for (Slot slot : containerScreen.getMenu().slots) {
            if (!(slot.container instanceof Inventory)) {
                continue;
            }
            int containerSlot = slot.getContainerSlot();
            if (!ModConfig.isProtected(protectedSlots, containerSlot)) {
                continue;
            }
            int slotX = left + slot.x;
            int slotY = top + slot.y;
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, OVERLAY_TINT);
        }
    }
}
