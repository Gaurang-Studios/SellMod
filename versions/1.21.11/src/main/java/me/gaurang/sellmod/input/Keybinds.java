package me.gaurang.sellmod.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.port.ConfigScreenPort;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class Keybinds {
    private static final KeyMapping.Category SELLMOD_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sellmod", "main"));
    public static KeyMapping TOGGLE;
    public static KeyMapping OPEN_CONFIG;

    private Keybinds() {
    }

    public static void register() {
        if (TOGGLE != null) {
            return;
        }
        TOGGLE = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.sellmod.toggle", InputConstants.Type.KEYSYM, -1, SELLMOD_CATEGORY));
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.sellmod.config", InputConstants.Type.KEYSYM, -1, SELLMOD_CATEGORY));
    }

    public static void handle(ConfigScreenPort configScreens) {
        if (TOGGLE == null) {
            return;
        }
        while (TOGGLE.consumeClick()) {
            ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled;
            ModConfig.INSTANCE.save();
        }
        while (OPEN_CONFIG.consumeClick()) {
            Minecraft client = Minecraft.getInstance();
            configScreens.open(client.screen);
        }
    }
}
