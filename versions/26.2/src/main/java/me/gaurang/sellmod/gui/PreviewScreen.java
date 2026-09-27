package me.gaurang.sellmod.gui;

import me.gaurang.sellmod.core.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PreviewScreen extends Screen {
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int COLS = 9;
    private static final int INVENTORY_ROWS = 4;
    private static final int EQUIPMENT_COUNT = 5;
    private static final int GRID_WIDTH = COLS * SLOT_SPACING;
    private static final int GRID_HEIGHT = INVENTORY_ROWS * SLOT_SPACING;
    private static final int EQUIPMENT_GAP = 24;
    private static final int TOTAL_WIDTH = GRID_WIDTH + EQUIPMENT_GAP + SLOT_SIZE;
    private static final int OVERLAY_TINT = 822033200;

    private final Screen returnScreen;
    private int gridLeft;
    private int gridTop;
    private int equipmentX;
    private int equipmentTop;
    private int hoveredSlot = -1;

    public PreviewScreen(Screen returnScreen) {
        super(Component.literal("Protected Slot Preview"));
        this.returnScreen = returnScreen;
    }

    @Override
    public void init() {
        gridLeft = (width - TOTAL_WIDTH) / 2;
        gridTop = (height - GRID_HEIGHT) / 2 - 18;
        equipmentX = gridLeft + GRID_WIDTH + EQUIPMENT_GAP;
        equipmentTop = gridTop;
    }

    private static int slotRow(int index) {
        return index < 9 ? 3 : (index - 9) / 9;
    }

    private static int slotCol(int index) {
        return index < 9 ? index : (index - 9) % 9;
    }

    private int slotX(int index) {
        return gridLeft + slotCol(index) * SLOT_SPACING;
    }

    private int slotY(int index) {
        return gridTop + slotRow(index) * SLOT_SPACING;
    }

    private int equipmentY(int index) {
        return equipmentTop + index * SLOT_SPACING;
    }

    private int getSlotIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < 36; i++) {
            int x = slotX(i);
            int y = slotY(i);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return i;
            }
        }
        for (int i = 0; i < 5; i++) {
            int index = equipmentIndex(i);
            int x = equipmentX;
            int y = equipmentY(i);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return index;
            }
        }
        return -1;
    }

    private static int equipmentIndex(int position) {
        // Preview order: helmet, chestplate, leggings, boots, offhand.
        return switch (position) {
            case 0 -> 39;
            case 1 -> 38;
            case 2 -> 37;
            case 3 -> 36;
            case 4 -> ModConfig.OFFHAND_PROTECTED_INDEX;
            default -> -1;
        };
    }

    private boolean[] normalizedProtectedSlots() {
        boolean[] protectedSlots = ModConfig.INSTANCE.protectedSlots;
        int required = ModConfig.OFFHAND_PROTECTED_INDEX + 1;
        if (protectedSlots == null || protectedSlots.length < required) {
            boolean[] migrated = new boolean[required];
            if (protectedSlots != null) {
                System.arraycopy(protectedSlots, 0, migrated, 0,
                        Math.min(protectedSlots.length, migrated.length));
            }
            protectedSlots = migrated;
            ModConfig.INSTANCE.protectedSlots = protectedSlots;
        }
        for (int index = 36; index < required; index++) {
            protectedSlots[index] = true;
        }
        return protectedSlots;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        // Only left-click toggles protection.
        if (event.button() != 0) {
            return super.mouseClicked(event, bl);
        }
        int index = getSlotIndexAt(event.x(), event.y());
        if (index < 0) {
            return super.mouseClicked(event, bl);
        }

        boolean[] protectedSlots = normalizedProtectedSlots();
        if (index >= 36) {
            // Armor and offhand are always protected.
            return true;
        }
        protectedSlots[index] = !protectedSlots[index];
        ModConfig.INSTANCE.save();
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        hoveredSlot = getSlotIndexAt(mouseX, mouseY);
        graphics.fill(0, 0, width, height, 0xA0000000);
        drawCenteredText(graphics, "Protected Slot Preview", width / 2, gridTop - 26, 0xFFFFFF);
        drawCenteredText(graphics, "Left-click inventory slots to toggle protection", width / 2,
                gridTop - 14, 0xBFBFBF);

        boolean[] protectedSlots = normalizedProtectedSlots();
        for (int i = 0; i < 36; i++) {
            drawSlot(graphics, i, protectedSlots, slotX(i), slotY(i));
        }
        for (int i = 0; i < EQUIPMENT_COUNT; i++) {
            int index = equipmentIndex(i);
            drawSlot(graphics, index, protectedSlots, equipmentX, equipmentY(i));
        }

        drawCenteredText(graphics, "Armor + offhand are always protected", width / 2,
                gridTop + GRID_HEIGHT + 6, 0xA0A0A0);
        drawCenteredText(graphics, "ESC to return.", width / 2,
                gridTop + GRID_HEIGHT + 18, 0xA0A0A0);

        if (hoveredSlot >= 0) {
            int x;
            int y;
            if (hoveredSlot < 36) {
                x = slotX(hoveredSlot);
                y = slotY(hoveredSlot);
            } else {
                int position = equipmentPosition(hoveredSlot);
                x = equipmentX;
                y = equipmentY(position);
            }
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x40FFFFFF);
        }
    }

    private static int equipmentPosition(int index) {
        return switch (index) {
            case 39 -> 0;
            case 38 -> 1;
            case 37 -> 2;
            case 36 -> 3;
            case ModConfig.OFFHAND_PROTECTED_INDEX -> 4;
            default -> 0;
        };
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int index, boolean[] protectedSlots, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF1B1B1B);
        ItemStack stack = getInventoryStack(index);
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y, index);
            graphics.itemDecorations(this.font, stack, x, y, null);
        }
        if (index < protectedSlots.length && protectedSlots[index]) {
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, OVERLAY_TINT);
        }
    }

    private static ItemStack getInventoryStack(int index) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = client.player.getInventory();
        if (index < 0 || index > ModConfig.OFFHAND_PROTECTED_INDEX) {
            return ItemStack.EMPTY;
        }
        return inventory.getItem(index);
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color);
    }

    @Override
    public void onClose() {
        ModConfig.INSTANCE.save();
        if (returnScreen != null) {
            Minecraft.getInstance().gui.setScreen(returnScreen);
        } else {
            super.onClose();
        }
    }
}
