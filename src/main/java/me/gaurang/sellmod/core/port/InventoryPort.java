package me.gaurang.sellmod.core.port;

/**
 * Inventory/menu operations. All methods act on the player's currently open
 * container menu; identity tracking is handled through {@link WindowPort}.
 */
public interface InventoryPort {
    /** Live snapshot of the currently open container menu. */
    MenuSnapshot snapshot();

    /**
     * Whether the destination slot accepts the source stack. Evaluated live so
     * server corrections within the same tick are reflected, matching the
     * original controller's direct {@code Slot.mayPlace} checks.
     */
    boolean mayPlaceOn(int targetIndex, int sourceIndex);

    /** Performs a pickup or quick-move click on the given click index. */
    void click(int slotIndex, ClickKind kind);
}
