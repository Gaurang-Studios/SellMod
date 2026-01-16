package me.gaurang.sellmod.config;

public class ModConfig {

    public static final ModConfig INSTANCE = new ModConfig();

    // Master toggle
    public boolean enabled = false;

    // Command used to open the sell GUI
    public String sellCommand = "/sellgui";

    // Base delay between sell cycles (seconds)
    public int baseDelaySeconds = 5;
    
    // Delay randomization toggle (±2s)
    public boolean randomizeDelay = true;

    // Optional GUI title verification
    public boolean requireTitleMatch = false;
    public String expectedTitle = "";

    private ModConfig() {
    }
}