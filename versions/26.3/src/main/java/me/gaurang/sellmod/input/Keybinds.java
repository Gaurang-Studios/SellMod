package me.gaurang.sellmod.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.port.ConfigScreenPort;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/**
 * 26.3 keybinds (SDL input era). Two proven breaks vs 26.2, both confine here:
 * <ul>
 *   <li>{@code InputConstants.Type.KEYSYM} was removed with GLFW; vanilla 26.3
 *   itself registers all keybinds with {@code KEYBOARD} (verified: 23 uses in
 *   {@code Options}, 0 of {@code KEYSYM}).</li>
 *   <li>The unbound-key sentinel changed: {@code -1} ({@code GLFW_KEY_UNKNOWN})
 *   is now a real key value. {@code UNKNOWN = KEYBOARD.getOrCreate(0)}
 *   (interned via {@code computeIfAbsent}) and
 *   {@code KeyMapping.isUnbound() <=> key.equals(UNKNOWN)}, so the default key
 *   code here is {@code 0}, preserving exact 26.2 behavior (unbound default).</li>
 * </ul>
 */
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
                new KeyMapping("key.sellmod.toggle", InputConstants.Type.KEYBOARD, 0, SELLMOD_CATEGORY));
        OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.sellmod.config", InputConstants.Type.KEYBOARD, 0, SELLMOD_CATEGORY));
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
            configScreens.open(client.gui.screen());
        }
    }
}
