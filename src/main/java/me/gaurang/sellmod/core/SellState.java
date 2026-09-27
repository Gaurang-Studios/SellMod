package me.gaurang.sellmod.core;

public enum SellState {
    IDLE,
    SEND_COMMAND,
    WAIT_FOR_GUI,
    MOVE_ITEMS,
    CLOSE_GUI,
    CLICK_SELL_BUTTON,
    WAIT_FOR_BUTTON,
    COOLDOWN
}
