# EasyGame Minecraft Mod (Fabric 26.1)

A high-performance quality-of-life and enhancement mod designed to streamline trading, enchantments, crafting, and trial chambers.

## ✨ Features

### 1. Unlimited Villager Trading
- Villagers never lock out trades or run out of stock.
- Trade limits are removed so you can trade indefinitely.

### 2. Maximum Enchantment Levels Only
- All enchanted book trades and enchanted equipment from villagers are guaranteed to be at their absolute maximum tier (e.g., **Sharpness V**, **Efficiency V**, **Unbreaking III**, **Fortune III**, **Protection IV**, **Mending I**).
- No lower tier enchants (I, II, III) from trading.

### 3. Emeralds Only for Enchanted Books
- Librarians no longer require a regular Book (`minecraft:book`) to trade for Enchanted Books.
- You can trade directly with emeralds only!

### 4. Fletcher Super-Trades
- **1 Stick ➔ 1 Emerald** (No more grinding 32 sticks for a single emerald).
- **1 Stick ➔ 2 Apples** (Effortless apple supply for food and crafting).

### 5. Toolsmith Armor Trims
- Toolsmiths trade **all 18 vanilla armor trim smithing templates** for **2 Diamonds each**.
- Easily accessible directly in the trade list or via the **🛡 Trims** button in the trading GUI.

### 6. Interactive Trade Cycling & Trade Locking (Workstation Refresh)
- **⟳ Cycle Trades Button**: Reroll trades instantly right inside the Villager trading GUI without having to place, break, or replace lecterns and workstations!
- **★ Lock / Save Trade Button**: Lock your favorite trades. When you click **Cycle**, saved trades stay permanently locked in place while unlocked trades are rerolled!

### 7. Enchantment Descriptions in Tooltips
- Hovering over any enchanted book, weapon, armor, or tool displays an informative tooltip describing exactly what each enchantment does in clear, formatted text.

### 8. Craftable Enchanted Golden Apples
- The standard golden apple recipe (**8 Gold Ingots + 1 Apple**) crafts an **Enchanted Golden Apple** (`minecraft:enchanted_golden_apple`).

### 9. Trial Chamber & Ominous Vault Overhauls
- **Breeze Drops**: Every Breeze drops **10 Ominous Trial Keys** upon defeat.
- **Infinite Vault Unlocks**: Ominous Vaults have no player reward limit; you can insert infinite keys into a single vault to receive continuous rewards.
- **Mace Heavy Core Boost**: The Heavy Core drop rate in Ominous Vaults is boosted to **29%**.

---

## 🛠 Building & CI

This repository is configured with GitHub Actions to automatically build and release the mod JAR artifact on push:
```bash
./gradlew build
```
Artifacts are automatically published under GitHub Actions artifacts.
