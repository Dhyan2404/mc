# EasyGame Minecraft Mod (Fabric 26.1 / Java 25)

A complete, production-ready quality-of-life and gameplay overhaul mod for **Minecraft Java Edition 26.1**, built with **Fabric Loader**, **Fabric API**, and **Fabric Language Kotlin**.

---

## 🌟 Core Gameplay Features

### 1. Trading Currency & Item Cost Overrides
- **Emerald-Only Trades:**
  - Removes all secondary item costs from villager trades (e.g., normal book requirements for Librarian enchanted book trades, compass requirements for Cartographers, etc.).
  - Purchasable items strictly require Emeralds only.
- **Fletcher Trade Customization:**
  - **Sell:** `1 Stick ➔ 1 Emerald` (replaces and overrides the default 32 sticks trade).
  - **Buy / Barter:** `1 Stick ➔ 2 Apples` (for easy food and golden apple crafting).
  - Automatically disables/overrides conflicting vanilla stick trades.
- **Toolsmith Armor Trim Interface:**
  - Dedicated **`[ 🛡 Trims ]`** button rendered strictly when interacting with a Toolsmith.
  - Clicking this button replaces the trade pool with **all 18 Minecraft Armor Trim Smithing Templates**:
    - Coast, Dune, Eye, Host, Raiser, Rib, Sentry, Shaper, Silence, Snout, Spire, Tide, Vex, Ward, Wayfinder, Wild, Flow, and Bolt.
  - Cost per trim: **Exactly 2 Diamonds** per template.

---

### 2. Villager Trade Cycling & Trade Locking System
- **GUI Cycle Button (`[ ⟳ Cycle ]`):**
  - Injected directly into the top bar of the vanilla `MerchantScreen` (Villager Trading GUI).
  - Instantly rerolls and refreshes the villager's available trade pool without needing to break and replace workstations (Lectern, Fletching Table, Smithing Table, etc.).
- **Trade Locking / Save Mechanism (`[ ★ Saved ] / [ ☆ Save ]`):**
  - Interactive toggle button in the trading GUI dynamically linked to the currently selected trade offer.
  - When an offer is marked as **Saved / Locked**, cycling the trade pool regenerates all unlocked slots while preserving locked trades completely untouched.
  - Cleanly rerolls unlocked trades without corrupting villager NBT data, XP, or leveling progression.

---

### 3. Experience & Anvil Mechanics
- **Trade XP Override:**
  - Every single completed villager trade grants **exactly 3 full levels/bars** of player experience points consistently.
  - Vanilla's default small XP orb drop is suppressed to ensure consistent 3-level increments.
- **Flat Anvil Repair & Combining Cost:**
  - Prior-work penalties (`REPAIR_COST`) are completely reset to `0` on anvil operations.
  - All anvil operations (combining, repairing, renaming, applying enchanted books) strictly require **exactly 1 Level of XP**.
  - The "Too Expensive!" cap is completely eliminated.

---

### 4. Additional Enhancements
- **Maximum Enchantment Levels Only:**
  - All enchanted books and enchanted tools/armor offered by villagers are guaranteed to be at their maximum level (Sharpness V, Efficiency V, Unbreaking III, Protection IV, Fortune III, Mending I, etc.).
- **Enchantment Tooltip Descriptions:**
  - Hovering over enchanted books or gear displays rich tooltips explaining exactly what each enchantment does.
- **Enchanted Golden Apple Recipe:**
  - Standard recipe (**8 Gold Ingots + 1 Apple**) crafts an **Enchanted Golden Apple** (`minecraft:enchanted_golden_apple`).
- **Trial Chamber & Ominous Vaults:**
  - **Breeze Drops:** Every Breeze drops **10 Ominous Trial Keys** on death.
  - **Infinite Ominous Vaults:** Vault reward lockout removed; players can use infinite keys in a single vault.
  - **Heavy Core Boost:** Mace Heavy Core drop chance increased to **29%**.

---

## 📁 Project Architecture

```
minecraft mod/
├── .github/workflows/
│   └── build.yml                     # GitHub Actions CI for automated build & artifact upload
├── src/
│   ├── main/
│   │   ├── java/com/easygame/
│   │   │   ├── mixin/
│   │   │   │   ├── AnvilMenuMixin.java          # Flat 1 XP cost & removes prior-work penalty
│   │   │   │   ├── MerchantMenuAccessor.java    # Accessor for container trader
│   │   │   │   ├── MerchantOfferMixin.java      # Emerald-only, single stick cost, unlimited uses
│   │   │   │   ├── MerchantResultSlotMixin.java # Gives 3 full levels per trade
│   │   │   │   ├── VaultServerDataMixin.java    # Infinite vault unlocks
│   │   │   │   └── VillagerMixin.java           # Auto-applies Fletcher & enchantment overrides
│   │   │   ├── network/
│   │   │   │   └── CycleTradesPayload.java      # C2S CustomPacketPayload for trade cycling
│   │   │   └── trade/
│   │   │       └── TradeHelper.java             # Core cycling logic, trim loading, lock preservation
│   │   ├── kotlin/com/easygame/
│   │   │   └── EasyGame.kt                      # Mod initializer, packet registry, Breeze drops
│   │   └── resources/
│   │       ├── data/                            # Recipes and Trial Chamber loot tables
│   │       ├── easygame.mixins.json             # Mixin configuration
│   │       └── fabric.mod.json                  # Mod metadata & dependencies
│   └── client/
│       ├── java/com/easygame/client/mixin/
│       │   └── MerchantScreenMixin.java         # Cycle, Save, and Toolsmith Trim GUI buttons
│       ├── kotlin/com/easygame/client/
│       │   ├── EasyGameClient.kt                # Client initializer, tooltip callbacks, lock state
│       │   └── EnchantmentDescriptions.kt       # Descriptions dictionary for all vanilla enchants
│       └── resources/
│           └── easygame.client.mixins.json      # Client mixin configuration
├── build.gradle.kts                             # Loom 1.18.2, Java 25, Kotlin 2.4.20
├── gradle.properties                            # Minecraft 26.1, Fabric Loader 0.19.5
└── settings.gradle.kts
```

---

## 🚀 Building & Installing

### Prerequisites
- **JDK 25** (Microsoft or Eclipse Temurin distribution)
- **Minecraft 26.1** with **Fabric Loader >= 0.19.5**
- **Fabric API** installed in your `.minecraft/mods` folder

### Compiling Locally
To compile the mod into a production-ready JAR:
```bash
# On Linux/macOS
./gradlew build

# On Windows (CMD / PowerShell)
gradlew.bat build
```
The compiled mod JAR will be output to:
```
build/libs/easygame-1.0.0.jar
```

### Automated GitHub CI
Pushes to `main` automatically trigger `.github/workflows/build.yml` which validates, compiles, and uploads the built `.jar` file directly under the GitHub repository's **Actions ➔ Artifacts** tab.

### Installation
1. Place the generated `easygame-1.0.0.jar` into your Minecraft `.minecraft/mods` folder.
2. Launch Minecraft using the **Fabric 26.1** profile.
