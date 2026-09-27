package me.gaurang.sellmod.adapter;

import me.gaurang.sellmod.core.port.ClickKind;
import me.gaurang.sellmod.core.port.InventoryPort;
import me.gaurang.sellmod.core.port.MenuSlot;
import me.gaurang.sellmod.core.port.MenuSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/** 26.x container-menu port over the live {@code player.containerMenu}. */
public class FabricInventory implements InventoryPort {

    @Override
    public MenuSnapshot snapshot() {
        List<MenuSlot> slots = new ArrayList<>();
        int carriedCount = 0;
        var menu = currentMenu();
        if (menu != null) {
            for (Slot slot : menu.slots) {
                slots.add(toView(slot));
            }
            ItemStack carried = menu.getCarried();
            carriedCount = carried.isEmpty() ? 0 : Math.max(1, carried.getCount());
        }
        return new MenuSnapshot(List.copyOf(slots), carriedCount);
    }

    @Override
    public boolean mayPlaceOn(int targetIndex, int sourceIndex) {
        var menu = currentMenu();
        if (menu == null) {
            return false;
        }
        Slot target = findByIndex(menu, targetIndex);
        Slot source = findByIndex(menu, sourceIndex);
        return target != null && source != null && target.mayPlace(source.getItem());
    }

    @Override
    public void click(int slotIndex, ClickKind kind) {
        FabricCommands.click(slotIndex, kind);
    }

    private static MenuSlot toView(Slot slot) {
        ItemStack stack = slot.getItem();
        String itemId = slot.hasItem()
                ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                : "";
        String itemPath = slot.hasItem()
                ? BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                : "";
        StringBuilder text = new StringBuilder();
        if (slot.hasItem()) {
            // Identical composition to the original detector input:
            // display name followed by every lore line separated by spaces.
            text.append(stack.getHoverName().getString());
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    text.append(' ').append(line.getString());
                }
            }
        }
        return new MenuSlot(
                slot.index,
                slot.container instanceof Inventory,
                slot.hasItem(),
                stack.getCount(),
                slot.getContainerSlot(),
                itemId,
                itemPath,
                text.toString()
        );
    }

    private static Slot findByIndex(AbstractContainerMenu menu, int index) {
        for (Slot slot : menu.slots) {
            if (slot.index == index) {
                return slot;
            }
        }
        return null;
    }

    private static AbstractContainerMenu currentMenu() {
        var client = Minecraft.getInstance();
        return client.player == null ? null : client.player.containerMenu;
    }
}
