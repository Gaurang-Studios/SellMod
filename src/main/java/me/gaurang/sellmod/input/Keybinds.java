package me.gaurang.sellmod.input;

import me.gaurang.sellmod.config.ModConfig;
import me.gaurang.sellmod.config.ModConfigScreen;
import me.gaurang.sellmod.controller.SellController;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    private static final KeyMapping.Category SELLMOD_CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath("sellmod", "main")
            );

    public static KeyMapping TOGGLE;
    public static KeyMapping OPEN_CONFIG;

    private Keybinds() {}

    public static void register() {
        if (TOGGLE != null) return;

        TOGGLE = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.sellmod.toggle",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        SELLMOD_CATEGORY
                )
        );

        OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.sellmod.config",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        SELLMOD_CATEGORY
                )
        );
    }

    public static void handle(SellController controller) {
        if (TOGGLE == null) return;

        while (TOGGLE.consumeClick()) {
            ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled;
        }

        while (OPEN_CONFIG.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    ModConfigScreen.create(net.minecraft.client.Minecraft.getInstance().screen)
            );
        }
    }
}