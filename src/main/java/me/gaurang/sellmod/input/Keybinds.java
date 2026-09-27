package me.gaurang.sellmod.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.port.ConfigScreenPort;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

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
        TOGGLE = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.sellmod.toggle", InputConstants.Type.KEYSYM, -1, SELLMOD_CATEGORY));
        OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
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
