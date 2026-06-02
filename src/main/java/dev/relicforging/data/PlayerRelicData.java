package dev.relicforging.data;

import dev.relicforging.api.RelicType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the plugin needs to know about one player's relic right now.
 *
 * Design notes:
 *   - This object lives in memory (in RelicManager's map) while the player is online.
 *   - It is serialised to a flat YAML file under plugins/RelicForging/playerdata/<uuid>.yml
 *     when the player quits and deserialised when they join.
 *   - Cooldowns are stored as the server tick at which the cooldown *expires*, so they
 *     don't need special handling across save/load (we just compare to current tick).
 *   - Energy is stored as an integer; the regen task increments it every second.
 *
 * XP / Level tiers:
 *   Tier I  = levels  1–10
 *   Tier II = levels 11–20
 *   Tier III= levels 21–30
 */
public class PlayerRelicData {

    private final UUID playerId;

    // Which relic is currently equipped (null = none)
    private RelicType equippedType;

    // Current Resonance Energy (never exceeds maxEnergy for the equipped relic)
    private int energy;

    // Mastery XP for each relic (persisted so switching relics doesn't lose progress)
    private final Map<RelicType, Integer> xpMap     = new HashMap<>();
    private final Map<RelicType, Integer> levelMap  = new HashMap<>();

    // Cooldown tracking: key → tick timestamp when cooldown expires
    // Keys are "primary" and "secondary"
    private final Map<String, Long> cooldownExpiry = new HashMap<>();

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public PlayerRelicData(UUID playerId) {
        this.playerId = playerId;
        // Initialise XP/level for every relic type so we never get NPEs
        for (RelicType type : RelicType.values()) {
            xpMap.put(type, 0);
            levelMap.put(type, 1);
        }
    }

    // ----------------------------------------------------------------
    // Energy
    // ----------------------------------------------------------------

    public int getEnergy()                 { return energy; }
    public void setEnergy(int energy)      { this.energy = Math.max(0, energy); }

    /** Subtracts cost from energy (floored at 0). */
    public void consumeEnergy(int cost)    { this.energy = Math.max(0, this.energy - cost); }

    /** Adds regen, capped at the supplied max. Returns new energy value. */
    public int regenEnergy(int amount, int max) {
        this.energy = Math.min(max, this.energy + amount);
        return this.energy;
    }

    // ----------------------------------------------------------------
    // Cooldowns
    //   We store the *expiry* tick so a simple subtraction gives remaining ticks.
    // ----------------------------------------------------------------

    /**
     * Records a new cooldown.
     *
     * @param key         "primary" or "secondary"
     * @param durationTicks how many ticks the cooldown lasts
     */
    public void setCooldown(String key, int durationTicks) {
        long expiresAt = getCurrentTick() + durationTicks;
        cooldownExpiry.put(key, expiresAt);
    }

    /**
     * Returns how many ticks remain on the cooldown, or 0 if it has expired.
     */
    public long getCooldownRemaining(String key) {
        Long expiry = cooldownExpiry.get(key);
        if (expiry == null) return 0L;
        long remaining = expiry - getCurrentTick();
        return Math.max(0L, remaining);
    }


    public Map<String, Long> getCooldownExpiry() {
        return cooldownExpiry;
    }

    /** Returns the cooldown remaining in whole seconds (rounded up). */
    public int getCooldownSeconds(String key) {
        long ticks = getCooldownRemaining(key);
        return (int) Math.ceil(ticks / 20.0);
    }

    // ----------------------------------------------------------------
    // Relic equipped
    // ----------------------------------------------------------------

    public RelicType getEquippedType()              { return equippedType; }
    public boolean   isRelicEquipped()              { return equippedType != null; }

    public void setEquippedType(RelicType type) {
        this.equippedType = type;
    }

    public void clearEquipped() {
        this.equippedType = null;
        cooldownExpiry.clear();
    }

    // ----------------------------------------------------------------
    // XP and leveling
    // ----------------------------------------------------------------

    public int getXp(RelicType type)   { return xpMap.getOrDefault(type, 0); }
    public int getLevel(RelicType type){ return levelMap.getOrDefault(type, 1); }

    /**
     * Adds XP to the currently equipped relic (does nothing if none equipped).
     * Returns true if the relic levelled up as a result.
     */
    public boolean addXp(int amount) {
        if (equippedType == null) return false;
        int current = xpMap.getOrDefault(equippedType, 0) + amount;
        xpMap.put(equippedType, current);
        return checkLevelUp();
    }

    /**
     * Checks whether the accumulated XP warrants a level-up.
     * The XP threshold grows with level: threshold = base * (level ^ 1.4)
     * These constants mirror config defaults but are recomputed here at runtime
     * so reloading config also affects ongoing progression.
     */
    private boolean checkLevelUp() {
        if (equippedType == null) return false;
        int level = levelMap.getOrDefault(equippedType, 1);
        if (level >= 30) return false;          // already max level

        int xpNeeded = xpForLevel(level + 1);
        int currentXp = xpMap.getOrDefault(equippedType, 0);
        if (currentXp >= xpNeeded) {
            levelMap.put(equippedType, level + 1);
            xpMap.put(equippedType, currentXp - xpNeeded);   // carry over surplus XP
            return true;
        }
        return false;
    }

    /** XP required to reach the given level from the previous level. */
    public static int xpForLevel(int level) {
        return (int) (100 * Math.pow(level, 1.4));
    }

    // ----------------------------------------------------------------
    // Serialisation helpers (used by PlayerDataManager)
    // ----------------------------------------------------------------

    public UUID getPlayerId()                            { return playerId; }
    public Map<RelicType, Integer> getXpMap()            { return xpMap; }
    public Map<RelicType, Integer> getLevelMap()         { return levelMap; }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /**
     * Returns the current server tick count.
     * Paper's server tick counter is a stable, monotonic long that increments
     * exactly once per tick (every ~50 ms at 20 TPS), so it's ideal for
     * cooldown bookkeeping without needing System.currentTimeMillis().
     */
    private long getCurrentTick() {
        return org.bukkit.Bukkit.getCurrentTick();
    }
}
