package dev.relicforging.api;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for every relic type.
 *
 * Design intent:
 *   Each concrete relic (GaleRelic, EmberRelic, etc.) extends this class and
 *   implements only the three abstract methods: applyPassive, primaryAbility,
 *   and secondaryAbility.  All energy / cooldown bookkeeping is handled here
 *   so individual relics stay clean and focused on their unique gameplay logic.
 *
 * Lifecycle:
 *   The RelicManager holds one singleton instance of each Relic subclass.
 *   When a player equips a relic the manager calls applyPassive on a schedule.
 *   When the player uses /1 or /2 the manager calls useAbility(primary=true/false).
 */
public abstract class Relic {

    protected final RelicForgingPlugin plugin;

    // Values loaded from config — populated by RelicManager on startup.
    protected String displayName;
    protected int maxEnergy;
    protected int energyRegen;
    protected int primaryCost;
    protected int primaryCooldownTicks;
    protected String primaryName;
    protected int secondaryCost;
    protected int secondaryCooldownTicks;
    protected String secondaryName;

    protected Relic(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Abstract interface every relic must implement
    // ------------------------------------------------------------------

    /** Called once every passive-tick-interval while the relic is equipped. */
    public abstract void applyPassive(Player player, PlayerRelicData data);

    /** Called when the player removes the relic (cleans up potion effects etc.). */
    public abstract void removePassive(Player player);

    /** The /1 ability.  Energy and cooldown checks are done BEFORE this is called. */
    protected abstract void doPrimary(Player player, PlayerRelicData data);

    /** The /2 ability.  Energy and cooldown checks are done BEFORE this is called. */
    protected abstract void doSecondary(Player player, PlayerRelicData data);

    /** Which RelicType enum value this relic corresponds to. */
    public abstract RelicType getType();

    /**
     * The CustomModelData integer that the resource pack uses to redirect this
     * relic's visual to a custom model.  Each relic must return a unique value.
     *
     * Convention used in this plugin:
     *   GALE     = 1001
     *   EMBER    = 1002
     *   TIDE     = 1003
     *   ECHO     = 1004
     *   VANGUARD = 1005
     *   BURROW   = 1006
     *
     * These numbers are high enough to avoid accidental clashes with other
     * plugins that might also use CustomModelData on vanilla item types.
     */
    public abstract int getCustomModelData();

    // ------------------------------------------------------------------
    // Shared ability dispatcher — called by AbilityCommand
    // ------------------------------------------------------------------

    /**
     * Validates energy and cooldown, then delegates to doPrimary / doSecondary.
     *
     * @param player   the player firing the ability
     * @param data     the player's live relic data (energy, cooldowns, xp…)
     * @param primary  true = /1, false = /2
     * @return what happened so the command handler can give useful feedback
     */
    public final AbilityResult useAbility(Player player, PlayerRelicData data, boolean primary) {
        int cost        = primary ? primaryCost        : secondaryCost;
        int cooldown    = primary ? primaryCooldownTicks : secondaryCooldownTicks;
        String key      = primary ? "primary"          : "secondary";

        // --- cooldown check ---
        long remaining = data.getCooldownRemaining(key);
        if (remaining > 0) {
            return AbilityResult.ON_COOLDOWN;
        }

        // --- energy check ---
        if (data.getEnergy() < cost) {
            return AbilityResult.NOT_ENOUGH_ENERGY;
        }

        // --- fire the ability ---
        data.consumeEnergy(cost);
        data.setCooldown(key, cooldown);

        // Grant a small flat amount of mastery XP for ability use.
        data.addXp(8);

        if (primary) {
            doPrimary(player, data);
        } else {
            doSecondary(player, data);
        }

        return AbilityResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Item creation helpers
    // ------------------------------------------------------------------

    /**
     * Builds the ItemStack that physically represents this relic.
     * The relic type is stored in PersistentDataContainer so it survives restarts.
     */
    public ItemStack createItem() {
        org.bukkit.Material mat = getMaterial();
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', displayName));
        meta.setLore(buildLore());

        // Glow effect without an enchantment tooltip.
        // addEnchant goes on meta (not item) so it survives setItemMeta() below.
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        // Tell the resource pack which custom model to render for this item.
        // The integer must match the "predicate" value in the item's model JSON.
        meta.setCustomModelData(getCustomModelData());

        // Store relic type in NBT so we can always identify it
        meta.getPersistentDataContainer().set(
            RelicKeys.RELIC_TYPE,
            org.bukkit.persistence.PersistentDataType.STRING,
            getType().name()
        );

        item.setItemMeta(meta);
        return item;
    }

    protected org.bukkit.Material getMaterial() {
        // Subclasses may override to use a unique material/head texture.
        return org.bukkit.Material.NETHER_STAR;
    }

    private List<String> buildLore() {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + getTierDescription());
        lore.add("");
        lore.add("§e/1 §7— " + primaryName + " §8(" + primaryCost + " energy, " + ticksToSeconds(primaryCooldownTicks) + "s cd)");
        lore.add("§e/2 §7— " + secondaryName + " §8(" + secondaryCost + " energy, " + ticksToSeconds(secondaryCooldownTicks) + "s cd)");
        lore.add("");
        lore.add("§8Equip to activate passives");
        return lore;
    }

    private String getTierDescription() {
        switch (getType()) {
            case GALE:     return "Wind · Mobility · Exploration";
            case EMBER:    return "Fire · Combat · Aggression";
            case TIDE:     return "Water · Support · Utility";
            case ECHO:     return "Detection · Stealth · Utility";
            case VANGUARD: return "Tank · Protection · Support";
            case BURROW:   return "Mining · Excavation · Earth";
            case WARDEN: return "Darkness · Sonic · Control";
            case HOLLOW: return "Gravity · Spatial · Control";
            case PLAGUE: return "Decay · Infection · Spread";
            default:       return "Relic";
        }
    }

    private int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    // ------------------------------------------------------------------
    // Config loading (called by RelicManager)
    // ------------------------------------------------------------------

    /** Reads all values from the plugin config section for this relic. */
    public void loadConfig() {
        String base = "relics." + getType().configKey() + ".";
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();

        displayName             = cfg.getString(base + "display-name", "&fRelic");
        maxEnergy               = cfg.getInt(base + "max-energy", 100);
        energyRegen             = cfg.getInt(base + "energy-regen", 5);
        primaryCost             = cfg.getInt(base + "abilities.primary.energy-cost", 20);
        primaryCooldownTicks    = cfg.getInt(base + "abilities.primary.cooldown-ticks", 100);
        primaryName             = cfg.getString(base + "abilities.primary.name", "Primary");
        secondaryCost           = cfg.getInt(base + "abilities.secondary.energy-cost", 30);
        secondaryCooldownTicks  = cfg.getInt(base + "abilities.secondary.cooldown-ticks", 200);
        secondaryName           = cfg.getString(base + "abilities.secondary.name", "Secondary");
    }

    // ------------------------------------------------------------------
    // Getters used by managers / GUI
    // ------------------------------------------------------------------

    public String getDisplayName()           { return displayName; }
    public int    getMaxEnergy()             { return maxEnergy; }
    public int    getEnergyRegen()           { return energyRegen; }
    public int    getPrimaryCooldownTicks()  { return primaryCooldownTicks; }
    public int    getSecondaryCooldownTicks(){ return secondaryCooldownTicks; }
    public String getPrimaryName()           { return primaryName; }
    public String getSecondaryName()         { return secondaryName; }
}
