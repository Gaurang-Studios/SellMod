package me.gaurang.sellmod.core.port;

/**
 * Screen/container identity and lifecycle. Tokens are opaque identity handles
 * (the adapter returns the live screen/menu instances); the core only ever
 * compares them by reference equality, preserving the original controller's
 * screen-vs-menu swap semantics without importing Minecraft types.
 */
public interface WindowPort {
    /** Whether the current screen is an abstract container screen. */
    boolean containerScreenOpen();

    /** Identity token of the current screen, or null when no screen is open. */
    Object screenToken();

    /** Identity token of {@code screen.getMenu()}, or null when not applicable. */
    Object screenMenuToken();

    /** Identity token of the player's active menu ({@code player.containerMenu}). */
    Object activeMenuToken();

    /**
     * Closes the container only if the given token is still the active menu
     * (returning any carried items to the inventory).
     */
    void closeIfActive(Object menuToken);
}
