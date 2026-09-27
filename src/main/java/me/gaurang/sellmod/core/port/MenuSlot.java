package me.gaurang.sellmod.core.port;

/**
 * Immutable view of one slot inside the open container menu.
 * {@code index} is the click index used for container input;
 * {@code containerSlot} is the player-inventory container slot used by
 * protection rules.
 */
public record MenuSlot(
        int index,
        boolean playerOwned,
        boolean hasItem,
        int itemCount,
        int containerSlot,
        String itemId,
        String itemPath,
        String displayText
) {
}
