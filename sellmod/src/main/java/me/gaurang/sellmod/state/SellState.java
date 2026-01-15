package me.gaurang.sellmod.state;

public enum SellState {
    IDLE,
    SEND_COMMAND,
    WAIT_FOR_GUI,
    MOVE_ITEMS,
    CLOSE_GUI,
    COOLDOWN
}