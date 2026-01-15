package me.gaurang.sellmod.input;

import me.gaurang.sellmod.SellModClient;
import me.gaurang.sellmod.config.ModConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {

    public static KeyBinding KILL_SWITCH;
    public static KeyBinding TOGGLE_AUTOMATION;

    public static void register() {
        KILL_SWITCH = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.sellmod.kill_switch",
                        GLFW.GLFW_KEY_UNKNOWN, // unbound
                        "category.sellmod"
                )
        );

        TOGGLE_AUTOMATION = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.sellmod.toggle",
                        GLFW.GLFW_KEY_UNKNOWN, // unbound
                        "category.sellmod"
                )
        );
    }

    public static void handle() {
        // Kill switch
        while (KILL_SWITCH.wasPressed()) {
            SellModClient.SELL_CONTROLLER.disable();
            ModConfig.INSTANCE.enabled = false;
        }

        while (TOGGLE_AUTOMATION.wasPressed()) {
            if (SellModClient.SELL_CONTROLLER.isEnabled()) {
                SellModClient.SELL_CONTROLLER.disable();
                ModConfig.INSTANCE.enabled = false;
            } else {
                SellModClient.SELL_CONTROLLER.enable();
                ModConfig.INSTANCE.enabled = true;
            }
        }
    }

    private Keybinds() {}
}