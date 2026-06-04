package dev.relicforging.manager;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicKeys;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.relic.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * The central hub that ties everything together.
 *
 * What it does:
 *  1. Holds the singleton Relic instance for each RelicType.
 *  2. Runs the recurring passive-apply and energy-regen tasks.
 *  3. Provides helpers for equipping / unequipping relics so other classes
 *     (the inventory listener, admin commands, etc.) don't need to
 *     duplicate that logic.
 *  4. Exposes a getRelic(type) method so commands can reach the right relic
 *     without a giant switch statement scattered everywhere.
 *
 * Why one singleton per relic?
 *   Relic objects hold no mutable player state — they are stateless "behaviour"
 *   objects.  All player-specific data lives in PlayerRelicData.  So a single
 *   shared instance is safe and avoids needless object creation.
 */
public class RelicManager {

    private final RelicForgingPlugin plugin;

    // Map from RelicType to its singleton behaviour object
    private final Map<RelicType, Relic> relics = new EnumMap<>(RelicType.class);

    // Recurring scheduler tasks (kept so we can cancel them on disable)
    private BukkitTask passiveTask;
    private BukkitTask energyTask;

    public RelicManager(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Startup / Shutdown
    // ----------------------------------------------------------------

    /**
     * Registers all six relics, loads their config, and starts the background tasks.
     * Called once from RelicForgingPlugin#onEnable.
     */
    public void init() {
        // Register every relic.  Adding a new relic in the future is just:
        //   1. Write the class, 2. add one line here, 3. add config block.
        register(new GaleRelic(plugin));
        register(new EmberRelic(plugin));
        register(new TideRelic(plugin));
        register(new EchoRelic(plugin));
        register(new VanguardRelic(plugin));
        register(new BurrowRelic(plugin));
        register(new WardenRelic(plugin));
        register(new HollowRelic(plugin));
        register(new PlagueRelic(plugin));
        register(new ExecutionerRelic(plugin));
        register(new SoulboundRelic(plugin));
        register(new MobilityRelic(plugin));

        startTasks();
        plugin.getLogger().info("RelicManager started — " + relics.size() + " relics registered.");
    }

    private void register(Relic relic) {
        relic.loadConfig();
        relics.put(relic.getType(), relic);
    }

    /** Re-reads config for all relics (used by /relicadmin reload). */
    public void reloadConfig() {
        relics.values().forEach(Relic::loadConfig);
    }

    /**
     * Cancels all scheduled tasks.  Must be called from onDisable so the tasks
     * don't keep running after the plugin unloads (which would cause errors).
     */
    public void shutdown() {
        if (passiveTask != null) passiveTask.cancel();
        if (energyTask  != null) energyTask.cancel();
    }

    // ----------------------------------------------------------------
    // Background tasks
    // ----------------------------------------------------------------

    private void startTasks() {
        int passiveInterval = plugin.getConfig().getInt("passive-tick-interval", 20);
        int energyInterval  = plugin.getConfig().getInt("energy-regen-interval",  20);

        // Apply passive effects to every online player with a relic equipped.
        passiveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data == null || !data.isRelicEquipped()) continue;

                Relic relic = relics.get(data.getEquippedType());
                if (relic != null) relic.applyPassive(player, data);
            }
        }, passiveInterval, passiveInterval);

        // Regenerate energy for every online player with a relic equipped.
        energyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
                if (data == null || !data.isRelicEquipped()) continue;

                Relic relic = relics.get(data.getEquippedType());
                if (relic == null) continue;

                int newEnergy = data.regenEnergy(relic.getEnergyRegen(), relic.getMaxEnergy());

                // Refresh the action bar so the player can always see their energy.
                if (plugin.getConfig().getBoolean("show-action-bar", true)) {
                    updateActionBar(player, data, relic);
                }
            }
        }, energyInterval, energyInterval);
    }

    // ----------------------------------------------------------------
    // Equip / Unequip
    // ----------------------------------------------------------------

    /**
     * Equips a relic for a player: sets their data, applies the initial passive.
     * Called from the inventory listener when the player puts a relic in their
     * off-hand slot (we use off-hand as the dedicated relic slot for simplicity
     * since Paper doesn't allow truly custom inventory slots without a resource pack).
     */
    public void equipRelic(Player player, RelicType type) {
        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) return;

        // If they had a different relic on, remove its passives first.
        if (data.isRelicEquipped() && data.getEquippedType() != type) {
            Relic old = relics.get(data.getEquippedType());
            if (old != null) old.removePassive(player);
        }

        data.setEquippedType(type);

        Relic relic = relics.get(type);
        if (relic != null) {
            relic.applyPassive(player, data);
            player.sendMessage("§a✦ " + relic.getDisplayName() + " §aequipped!");
        } else {
            player.sendMessage("§a✦ Relic equipped!");
        }
        player.sendMessage("§7Use §e/1 §7and §e/2 §7to activate abilities.");
    }

    /**
     * Unequips the current relic: removes passives and clears player data.
     */
    public void unequipRelic(Player player) {
        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || !data.isRelicEquipped()) return;

        Relic relic = relics.get(data.getEquippedType());
        if (relic != null) relic.removePassive(player);

        data.clearEquipped();
        player.sendMessage("§7Relic unequipped.");
    }

    // ----------------------------------------------------------------
    // Action bar display
    // ----------------------------------------------------------------

    /**
     * Sends an action bar message showing energy level and ability cooldowns.
     *
     * Example: "§bGale Relic  §f⚡ 80/100  §e/1 ready  §e/2 12s"
     */
    public void updateActionBar(Player player, PlayerRelicData data, Relic relic) {
        StringBuilder sb = new StringBuilder();
        sb.append(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', relic.getDisplayName()));
        sb.append("  §f⚡ ").append(data.getEnergy()).append("/").append(relic.getMaxEnergy());

        int p = data.getCooldownSeconds("primary");
        sb.append("  §e/1 ");
        sb.append(p > 0 ? "§c" + p + "s" : "§aready");

        int s = data.getCooldownSeconds("secondary");
        sb.append("  §e/2 ");
        sb.append(s > 0 ? "§c" + s + "s" : "§aready");

        // Use Adventure Component API (Paper 1.21 native) instead of deprecated BungeeCord TextComponent.
        String legacyText = sb.toString();
        net.kyori.adventure.text.Component component =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(legacyText);
        player.sendActionBar(component);
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    /** Returns the Relic behaviour object for the given type, or null if not registered. */
    public Relic getRelic(RelicType type) {
        return relics.get(type);
    }

    /** Returns all registered relics (used by the GUI to list them). */
    public Collection<Relic> getAllRelics() {
        return Collections.unmodifiableCollection(relics.values());
    }

    /**
     * Reads a relic item's type from its PersistentDataContainer.
     * Returns null if the item is null or is not a relic.
     */
    public static RelicType getRelicTypeFromItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta()
            .getPersistentDataContainer()
            .get(RelicKeys.RELIC_TYPE, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return RelicType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
