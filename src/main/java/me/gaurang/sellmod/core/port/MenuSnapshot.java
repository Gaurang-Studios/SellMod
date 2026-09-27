package me.gaurang.sellmod.core.port;

import java.util.List;

/**
 * Point-in-time view of the live container menu. The adapter resolves this
 * against the player's current menu, mirroring the original controller's
 * direct reads of the live handler.
 */
public record MenuSnapshot(List<MenuSlot> slots, int carriedCount) {
}
