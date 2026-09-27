package me.gaurang.sellmod.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Version-independent configuration model. The config directory is injected by
 * the loader adapter; this class never references Minecraft or Fabric types.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configFile;
    private static boolean initialized;

    public int configVersion = 3;

    public boolean analyticsEnabled = true;
    public String analyticsEndpoint = "https://sellmod-analytics.onrender.com/api/events";
    public String analyticsApiKey = "sellmod-secret-key";
    public boolean enabled = false;
    public String sellCommand = "/sellgui";
    public TransferMode transferMode = TransferMode.SHIFT;
    public boolean sellThroughButton = false;
    public int baseDelaySeconds = 5;
    public int itemMoveDelayTicks = 4;
    public int transferBurst = 3;
    public boolean randomizeItemDelay = true;
    public boolean randomizeDelay = true;

    // Sell-through-button settings.
    public ButtonDetectionMode buttonDetectionMode = ButtonDetectionMode.HYBRID_AUTOMATIC;
    public String sellButtonMaterial = "minecraft:lime_dye";
    public int buttonClickDelayTicks = 2;
    public boolean randomizeButtonDelay = true;
    public int buttonProcessingDelayTicks = 4;

    /** 0-35 = player inventory/hotbar, 36-39 = armor, 40 = offhand. */
    public boolean[] protectedSlots = createDefaultProtectedSlots();

    // Vanilla player-inventory container-slot index of the offhand slot
    // (parity with the vanilla Inventory.SLOT_OFFHAND constant).
    public static final int OFFHAND_PROTECTED_INDEX = 40;
    private static final int PROTECTED_SLOT_COUNT = OFFHAND_PROTECTED_INDEX + 1;

    public static ModConfig INSTANCE;

    private ModConfig() {
    }

    /** Resolves and loads the config file from the given config directory. Idempotent. */
    public static synchronized void init(Path configDir) {
        if (initialized) {
            return;
        }
        initialized = true;
        configFile = configDir.resolve("sellmod.json");
        INSTANCE = load();
    }

    private static ModConfig load() {
        if (!Files.exists(configFile)) {
            ModConfig fresh = new ModConfig();
            fresh.save();
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded == null) {
                loaded = new ModConfig();
            }
            int previousVersion = loaded.configVersion;
            loaded.sanitize();

            // Version 3 adds sell-method/button settings. Gson's field defaults are
            // enough for old files, but we persist the new schema marker immediately.
            if (previousVersion < 3) {
                loaded.configVersion = 3;
                loaded.save();
            }
            return loaded;
        } catch (Exception ignored) {
            backupCorruptConfig();
            ModConfig fallback = new ModConfig();
            fallback.save();
            return fallback;
        }
    }

    private static void backupCorruptConfig() {
        try {
            if (Files.exists(configFile)) {
                Path backup = configFile.resolveSibling(configFile.getFileName() + ".bak");
                Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Keep the game running even if the backup cannot be created.
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configFile.getParent());
            Path tmp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Configuration changes should never crash the game.
        }
    }

    private void sanitize() {
        if (sellCommand == null || sellCommand.isBlank()) {
            sellCommand = "/sellgui";
        }
        if (analyticsEndpoint == null) {
            analyticsEndpoint = "";
        }
        if (analyticsApiKey == null) {
            analyticsApiKey = "";
        }
        if (transferMode == null) {
            transferMode = TransferMode.SHIFT;
        }
        if (buttonDetectionMode == null) {
            buttonDetectionMode = ButtonDetectionMode.HYBRID_AUTOMATIC;
        }
        if (sellButtonMaterial == null || sellButtonMaterial.isBlank()) {
            sellButtonMaterial = "minecraft:lime_dye";
        }

        baseDelaySeconds = Math.max(1, baseDelaySeconds);
        itemMoveDelayTicks = Math.max(1, Math.min(20, itemMoveDelayTicks));
        transferBurst = Math.max(1, Math.min(6, transferBurst));
        buttonClickDelayTicks = Math.max(1, Math.min(20, buttonClickDelayTicks));
        buttonProcessingDelayTicks = Math.max(1, Math.min(40, buttonProcessingDelayTicks));
        if (configVersion < 3) {
            configVersion = 3;
        }

        boolean[] normalized = new boolean[PROTECTED_SLOT_COUNT];
        if (protectedSlots != null) {
            System.arraycopy(protectedSlots, 0, normalized, 0,
                    Math.min(protectedSlots.length, normalized.length));
        }
        // Never allow automated selling to move worn armor or offhand.
        for (int slot = 36; slot < normalized.length; slot++) {
            normalized[slot] = true;
        }
        protectedSlots = normalized;
    }

    private static boolean[] createDefaultProtectedSlots() {
        boolean[] slots = new boolean[PROTECTED_SLOT_COUNT];
        Arrays.fill(slots, 36, PROTECTED_SLOT_COUNT, true);
        return slots;
    }

    public static boolean isProtected(boolean[] protectedSlots, int containerSlot) {
        if (containerSlot < 0) {
            return false;
        }
        // Equipment is always protected. This prevents armor/offhand from being sold
        // even if an older config predates explicit equipment protection.
        if (containerSlot >= 36 && containerSlot <= OFFHAND_PROTECTED_INDEX) {
            return true;
        }
        return protectedSlots != null
                && containerSlot < protectedSlots.length
                && protectedSlots[containerSlot];
    }

    public enum TransferMode {
        PICKUP,
        SHIFT
    }

    public enum ButtonDetectionMode {
        HYBRID_AUTOMATIC,
        MANUAL_MATERIAL
    }
}
