# RelicForging — Paper Plugin

A full MMO-style relic system for **Paper 1.21.4** (the newest stable release).
Six distinct relics, each with passive effects, two command-activated abilities,
automatic leveling (Tier I → II → III), a Resonance Energy system, and full
player data persistence.

---

## Build Requirements

- **Java 21** (the version Paper 1.21.4 requires)
- **Apache Maven** 3.8+

---

## How to Build

```bash
# From the RelicForging directory:
mvn clean package
```

Maven will download the Paper 1.21.4 API from PaperMC's repository, compile
all sources, and produce the final JAR at:

```
target/RelicForging-1.0.0.jar
```

Copy that JAR into your server's `plugins/` folder and restart.

---

## Project Structure

```
src/main/java/dev/relicforging/
│
├── RelicForgingPlugin.java       ← Main plugin entry point; wires everything together
│
├── api/
│   ├── AbilityResult.java        ← Enum returned by every ability (SUCCESS, ON_COOLDOWN, etc.)
│   ├── Relic.java                ← Abstract base class all relics extend
│   ├── RelicKeys.java            ← All NBT NamespacedKeys in one place
│   └── RelicType.java            ← Enum of the six relic archetypes
│
├── command/
│   ├── AbilityCommand.java       ← Handles /1 and /2
│   ├── RelicCommand.java         ← /relic xp, info, energy, help
│   ├── RelicsMenuCommand.java    ← /relics chest GUI
│   └── RelicAdminCommand.java    ← /relicadmin give, reset, reload
│
├── data/
│   ├── PlayerRelicData.java      ← Per-player runtime state (energy, cooldowns, xp)
│   └── PlayerDataManager.java    ← YAML persistence (load on join, save on quit)
│
├── listener/
│   ├── PlayerConnectionListener.java  ← Load/save on join/quit
│   ├── RelicInventoryListener.java    ← Detects off-hand equip/unequip
│   ├── RelicDamageListener.java       ← Fall damage, bonus damage, projectile reduction
│   └── RelicMenuListener.java         ← Prevents item theft from /relics GUI
│
├── manager/
│   └── RelicManager.java         ← Registers all relics, runs passive/regen ticks
│
└── relic/
    ├── GaleRelic.java            ← Wind Dash + Sky Leap
    ├── EmberRelic.java           ← Flame Burst + Inferno Wave
    ├── TideRelic.java            ← Tidal Push + Ocean Blessing
    ├── EchoRelic.java            ← Ore Pulse + Resonance Scan
    ├── VanguardRelic.java        ← Fortify + Shield Slam
    └── BurrowRelic.java          ← Vein Break + Tremor
```

---

## First Steps After Installing

1. **Give yourself a relic:**
   ```
   /relicadmin give YourName gale
   ```
2. **Equip it** — hold the item and press **F** (swap offhand) or drag it into your offhand slot in your inventory screen.
3. **Use abilities** with `/1` and `/2`.
4. **Check progress** with `/relic info` or `/relic xp`.
5. **Browse your collection** with `/relics`.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/1` | relicforging.use | Fire primary ability |
| `/2` | relicforging.use | Fire secondary ability |
| `/relic info` | relicforging.use | Show equipped relic stats |
| `/relic xp` | relicforging.use | Show mastery progress |
| `/relic energy` | relicforging.use | Check current energy |
| `/relics` | relicforging.use | Open relic collection GUI |
| `/relicadmin give <player> <relic>` | relicforging.admin | Give a relic item |
| `/relicadmin reset <player>` | relicforging.admin | Wipe player's relic data |
| `/relicadmin reload` | relicforging.admin | Hot-reload config.yml |

---

## The Six Relics

### Gale (Feather)
The mobility specialist. `Speed I` while sprinting, 70% fall damage reduction
(100% at level 10+). `/1` Wind Dash launches you forward and knocks nearby
enemies away. `/2` Sky Leap fires you upward with slow-falling to glide down.
Gains XP from distance traveled, parkour, and ability use.

### Ember (Blaze Rod)
The aggressive fighter. Short fire resistance after combat; bonus 20% damage
against burning targets (level 20+). `/1` Flame Burst ignites everything in a
4-block radius. `/2` Inferno Wave fires a 45-degree cone of fire. Gains XP from
killing mobs and dealing fire damage.

### Tide (Heart of the Sea)
The water support. Dolphin's Grace + water breathing + regen while swimming.
`/1` Tidal Push shoves all nearby entities away with knockback. `/2` Ocean
Blessing grants nearby allies swim speed and regen (extinguishes fire at level 20+,
grants Resistance at level 30). Gains XP from swimming and being underwater.

### Echo (Echo Shard)
The spelunker and scout. Night vision underground. `/1` Ore Pulse scans a
12-block sphere (18 at level 10+) and marks every ore with sculk particles. `/2`
Resonance Scan reveals all nearby entities through walls (applies Glowing at
level 20+). Gains XP from mining and cave exploration.

### Vanguard (Iron Chestplate)
The team tank. Permanent Resistance I passive; projectile damage reduced 30%.
`/1` Fortify grants Absorption and Resistance II to nearby allies (radius
doubles at level 30). `/2` Shield Slam knocks all nearby enemies away and
applies Slowness (adds Nausea at level 20+). Gains XP from taking damage and
protecting allies.

### Burrow (Iron Pickaxe)
The miner. Haste underground + ambient ore detection particles. `/1` Vein Break
flood-fills and instantly mines an entire connected ore vein (up to 16 blocks,
32 at level 10+), respecting Fortune/Silk Touch on your held tool. `/2` Tremor
slows all nearby enemies and lifts them off the ground with a seismic effect.
Gains XP from mining and underground time.

---

## Leveling System

Relics level automatically through use. XP requirements grow as:

```
xp_needed = 100 × level^1.4
```

The 30-level progression is divided into three tiers:

- **Tier I** (levels 1–10): base abilities
- **Tier II** (levels 11–20): enhanced abilities (larger radius, longer duration, etc.)
- **Tier III** (levels 21–30): signature upgrades unique to each relic

---

## Configuration

All numeric values in `plugins/RelicForging/config.yml` are live-reloadable
with `/relicadmin reload`. You can tune energy costs, cooldown durations, max
energy, and regen rates per relic without touching any Java code.

---

## Data Storage

Player progress is stored in `plugins/RelicForging/playerdata/<uuid>.yml`.
Data is loaded on join and saved on quit (and on server shutdown).
No database is required.

---

## Adding a New Relic (for developers)

The architecture is designed to make adding a seventh relic a few-minute task:

1. Add a new value to `RelicType`.
2. Create a class in `dev.relicforging.relic` that extends `Relic`.
3. Add a `register(new YourRelic(plugin))` line in `RelicManager.init()`.
4. Add a config block under `relics:` in `config.yml`.

That's it — the command system, leveling, energy, and GUI all pick it up automatically.
