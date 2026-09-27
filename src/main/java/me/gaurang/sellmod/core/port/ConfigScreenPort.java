package me.gaurang.sellmod.core.port;

/**
 * Opens the configuration screen from an arbitrary parent screen token.
 * Implemented by the loader/UI adapter (Cloth Config on 26.x lines).
 */
public interface ConfigScreenPort {
    void open(Object parentScreen);
}
