# SellMod

A Fabric client-side mod for Minecraft 1.21.8 that automates selling items
by interacting with server-provided sell GUIs.

## Features
- Opens sell GUI via configurable command
- Transfers items using cursor-based interactions
- Compatible with plugin-controlled shop GUIs
- Randomized delay between sell cycles
- Mod Menu + Cloth Config support

## How it works
SellMod simulates real player inventory interactions:
- Picks up items from the player inventory
- Places them into container slots that accept them
- Closes the GUI to trigger sell-on-close behavior

No QUICK_MOVE, no slot index assumptions.

## Important notes
- The mod dumps your entire inventory
- Keep important items out before enabling
- Behavior depends on server GUI design

## Requirements
- Fabric Loader 0.18+
- Fabric API
- Mod Menu
- Cloth Config

## Installation
1. Install Fabric Loader
2. Drop the mod JAR into your `mods` folder
3. Configure via Mod Menu

## License
MIT