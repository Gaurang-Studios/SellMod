package me.gaurang.sellmod.core.port;

/** Player/connection availability and command sending. */
public interface CommandPort {
    /** Mirrors {@code client.player != null}. */
    boolean playerAvailable();

    /** Mirrors {@code client.gameMode != null}. */
    boolean interactionsAvailable();

    /** Sends a chat command WITHOUT the leading slash. */
    void sendChatCommand(String commandWithoutSlash);

    /** Identity token of the player's active menu right now. */
    Object captureActiveMenuToken();
}
