# SellMod

[![Status](https://img.shields.io/badge/STATUS-RELEASE-2ecc71?style=for-the-badge)](https://github.com/Gaurang-Studios/SellMod/tags)
[![GitHub release](https://img.shields.io/github/v/release/Gaurang-Studios/SellMod?include_prereleases&style=for-the-badge)](https://github.com/Gaurang-Studios/SellMod/releases/latest)
[![GitHub issues](https://img.shields.io/github/issues/Gaurang-Studios/SellMod?style=for-the-badge)](https://github.com/Gaurang-Studios/SellMod/issues)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/sellmod?style=for-the-badge&logo=modrinth&color=1bd96a)](https://modrinth.com/mod/sellmod)

---

## 🚀 About

SellMod is a Fabric **client-side mod** for Minecraft 1.21.X.

It helps selling items by interacting with server-based sell GUIs exactly like a player would.

No server mods required.

---

## ✨ Features

- Configurable Sell Command (`/sell`, `/sellgui`, etc.)
- Transfer Modes:
  - SHIFT (Quick Move)
  - PICKUP (Cursor-based transfer)
- Transfer Burst system (move multiple stacks per tick)
- Adjustable item transfer speed (tick-based)
- Optional delay randomization
- GUI-full auto close detection
- Stall detection (prevents stuck sell GUIs)
- Compatible with plugin-based chest sell systems
- 1.21.1 → 1.21.11 support (tested builds)

---

## ⚙️ How It Works

SellMod simulates real player behavior:

1. Sends the configured sell command  
2. Waits for the server GUI to open  
3. Moves items using valid slot interactions  
4. Closes the GUI to trigger sell-on-close mechanics  

No packet spoofing.  
No inventory injection.  
Only legitimate screen interactions.

---

## 🛠 Requirements

- Minecraft 1.21.X (see releases for exact supported versions)
- Fabric Loader 0.18+
- Fabric API
- Cloth Config
- Mod Menu

---

## 📦 Installation

1. Install Fabric Loader
2. Install required dependencies
3. Place SellMod + dependencies in your `mods` folder
4. Open Mod Menu → Configure SellMod
5. Bind a key and start selling

---

## ⚡ Performance Notes

- Burst increases transfer speed but may increase packet rate.
- If a server is strict, increase item delay or reduce burst.
- SHIFT and PICKUP modes exist for maximum compatibility.

---

## ⚠️ Important

- The mod transfers your entire inventory.
- Keep tools, armor, and valuables out of inventory before enabling.
- Server-specific sell GUI behavior may vary.

---

## 🐛 Issues / Feedback

- GitHub Issues:  
  https://github.com/Gaurang-Studios/SellMod/issues
- Discord: `real_gaurang`

---

## 📜 License

MIT License