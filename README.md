# SellMod

[![Status](https://img.shields.io/badge/STATUS-RELEASE-2ecc71?style=for-the-badge)](https://github.com/Gaurang-Studios/SellMod/releases)
[![Release](https://img.shields.io/modrinth/v/sellmod?label=RELEASE&style=for-the-badge&color=3a0ca3)](https://modrinth.com/mod/sellmod)
[![GitHub Issues](https://img.shields.io/github/issues/Gaurang-Studios/SellMod?style=for-the-badge)](https://github.com/Gaurang-Studios/SellMod/issues)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/sellmod?style=for-the-badge&logo=modrinth&color=1bd96a)](https://modrinth.com/mod/sellmod)

---

## About

SellMod is a Fabric client-side mod for Minecraft that automates selling items through server-based sell GUIs using normal Minecraft screen interactions.

No server-side mod is required.

### Supported Minecraft Versions

- Minecraft 1.21.9–1.21.11
- Minecraft 26.1
- Minecraft 26.2
- Minecraft 26.3

---

## Features

- Configurable sell command
- Sell on Close mode
- Sell Through Button mode
- SHIFT (Quick Move) transfer
- PICKUP (cursor-based) transfer
- Transfer Burst
- Adjustable item transfer delay
- Randomized item transfer delay
- Randomized sell cycle delay
- Automatic sell-button detection
- Manual sell-button material selection
- GUI-full detection
- Stall protection
- Cursor-safety protection
- Protected Slots
- Protected Slot Preview
- Always-protected armor and offhand slots
- Optional anonymous analytics
- Public analytics dashboard

---

## Sell Modes

### Sell on Close

SellMod opens the configured sell GUI and transfers items into the sell area.

When the sell area is full, or there are no transferable player items remaining, SellMod closes the GUI and waits for the configured cycle delay before opening it again.

The process continues until you manually stop SellMod using the configured toggle keybind.

Closing the sell GUI yourself does not permanently stop automation.

### Sell Through Button

Sell Through Button mode keeps the sell GUI open whenever possible.

SellMod fills the sell area, detects the GUI's Sell button, and clicks it when the sell input is ready.

After clicking the button, SellMod waits for the configured processing delay and then continues in the same GUI when possible.

If the server closes the GUI, SellMod resumes using its normal cooldown and reopen cycle.

You can choose between automatic button detection and manual material selection.

Automatic detection considers the button's text, lore, material, language-related keywords, position, and surrounding GUI context.

Manual detection lets you choose one exact item material for the Sell button, while GUI context is still used to select the appropriate matching button.

Button click delay can be configured and randomized independently.

If no valid Sell button can be confidently detected, Sell Through Button mode stops safely instead of silently falling back to Sell on Close.

---

## Transfer Modes

### SHIFT

Uses Minecraft's normal Quick Move / Shift-Click interaction to move items into the sell GUI.

### PICKUP

Uses cursor-based pickup interactions to move items into the sell GUI.

Both transfer modes support Transfer Burst.

---

## Protected Slots

Protected Slots lets you decide which normal inventory and hotbar slots SellMod is allowed to use while selling.

Open **Mod Menu → SellMod Config → Protected Slots → Open Preview** to open the visual editor.

### Protected Slot Preview

The preview shows your inventory and hotbar slots, along with your armor and offhand slots.

- Left-click a normal inventory or hotbar slot to toggle protection.
- Protected slots are shown with a red tint.
- The selected protection is saved automatically.
- Press ESC to return to the configuration screen.

Your armor and offhand are always protected and cannot be disabled.

Protected slots are never moved into the sell GUI by SellMod.

The protection is also visually shown on inventory/container screens, making it easier to see which slots are currently protected.

---

## Faster / More Flexible Selling

### Transfer Burst

Transfer Burst allows multiple item stacks to be moved during a single client tick.

Higher burst values can move items faster, while lower values give more conservative interaction timing.

### Item Transfer Delay

You can configure the delay between individual item movements.

Lower delays make transfers faster, while higher delays provide more spacing between interactions.

### Randomized Item Delay

Adds small timing variations between individual item transfers.

### Randomized Sell Cycle Delay

Adds small timing variations between complete sell cycles.

These settings can be combined to give you more control over how quickly SellMod interacts with the sell GUI.

---

## Safer Inventory Handling

Armor and offhand slots are always protected and cannot be sold by SellMod.

Protected Slots can additionally prevent specific inventory and hotbar slots from being used.

SellMod detects when the sell GUI is full and handles the transition according to the selected sell mode.

A temporarily empty inventory does not automatically mean automation is finished. This allows SellMod to continue working with inventories that are being continuously replenished.

Sell Through Button mode also includes cursor-safety handling so a button interaction does not intentionally leave the player stuck carrying an item on the cursor.

---

## Better Reliability

SellMod uses bounded retries when attempting to open the configured sell GUI. If the GUI cannot be opened successfully after the allowed attempts, automation is stopped instead of continuing indefinitely.

Stall protection prevents the selling process from remaining in a non-progressing state forever.

Sell Through Button mode fails closed when it cannot confidently identify a valid Sell button. It does not silently switch to Sell on Close and perform a different action.

Button mode also does not require the sell-input contents to visibly change after pressing the Sell button. This allows it to work with server GUIs that refresh or continuously replenish their contents.

---

## Configuration

SellMod can be configured through Mod Menu.

Available settings include:

- Global Enabled setting
- Sell command
- Transfer mode
- Base sell-cycle delay
- Item transfer speed
- Transfer Burst
- Randomized item transfer delay
- Randomized sell cycle delay
- Sell Through Button mode
- Button detection mode
- Sell button material for manual detection
- Button click delay
- Randomized button click delay
- Button processing delay
- Protected Slots
- Anonymous analytics

The **General** category contains the global Enabled setting and general selling controls.

The dedicated **Sell Through Button** category contains the button-specific settings.

The **Protected Slots** category contains the visual Protected Slot Preview.

---

## Analytics & Privacy

SellMod includes optional anonymous analytics.

When enabled, analytics measure overall usage and feature adoption without collecting personal or server-identifying information.

Analytics may record events such as:

- Items transferred into the sell GUI
- Items submitted through the sell action
- Completed sell cycles
- Sell method used
- Transfer method used
- Feature usage
- Anonymous client and session identifiers

SellMod does **not** collect:

- Minecraft username or UUID
- Server IP, hostname, or domain
- Server software
- Minecraft or SellMod version telemetry
- Inventory contents
- GUI contents
- Screenshots
- Chat
- Coordinates or worlds
- Arbitrary commands
- Filesystem paths

Analytics are designed to run asynchronously and without blocking the selling process.

Public Analytics Dashboard:

https://sellmod-analytics.onrender.com/dashboard

---

## How It Works

SellMod uses normal client-side screen interactions:

1. Sends the configured sell command.
2. Waits for the sell GUI.
3. Transfers items using the selected transfer mode.
4. Respects Protected Slots and always-protected equipment slots.
5. Detects when the sell input is ready.
6. Closes the GUI or presses the Sell button depending on the selected mode.
7. Continues using the configured timing.

SellMod does not inject items into inventories or spoof inventory contents.

---

## Requirements

- Minecraft 1.21.9–1.21.11, 26.1, 26.2, or 26.3
- Fabric Loader
- Fabric API
- Cloth Config
- Mod Menu

Use the dependency versions listed for your specific Minecraft version.

---

## Installation

1. Install Fabric Loader for your Minecraft version.
2. Install Fabric API, Cloth Config, and Mod Menu.
3. Download the SellMod JAR matching your exact Minecraft version.
4. Place SellMod and its dependencies in your `mods` folder.
5. Open Mod Menu and configure SellMod.
6. Use the configured keybind to start or stop automation.

---

## Performance

Transfer Burst can increase the number of item movements performed in a short period.

For servers with stricter interaction limits, increase the item transfer delay and reduce the burst size.

Randomized item and cycle delays can also be enabled when you want more variation in interaction timing.

---

## Important

SellMod is designed for server-based sell GUIs, and GUI layouts and button designs can vary between servers.

Automatic Sell button detection is designed to work across different button names, lore, materials, languages, positions, and surrounding GUI contexts, but individual servers may still behave differently.

Review your configuration and sell GUI before enabling automation.

Armor and offhand are always protected.

---

## Issues / Feedback

- GitHub Issues: https://github.com/Gaurang-Studios/SellMod/issues
- Discord Server: https://dsc.gg/gaurangstudios

---

## License

MIT License