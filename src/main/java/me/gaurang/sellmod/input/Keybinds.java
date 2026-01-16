package me.gaurang.sellmod.input;

import me.gaurang.sellmod.config.ModConfigScreen;
import me.gaurang.sellmod.controller.SellController;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static final KeyBinding TOGGLE = KeyBindingHelper.registerKeyBinding(
        new KeyBinding(
            "key.sellmod.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.sellmod"
        )
    );

    public static final KeyBinding OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(
        new KeyBinding(
            "key.sellmod.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.sellmod"
        )
    );

    public static void handle(SellController controller) {
        while (TOGGLE.wasPressed()) {
            if (controller.isEnabled()) {
                controller.disable();
            } else {
                controller.enable();
            }
        }

        while (OPEN_CONFIG.wasPressed()) {
            CLIENT.setScreen(
                ModConfigScreen.create(CLIENT.currentScreen)
            );
        }
    }
}