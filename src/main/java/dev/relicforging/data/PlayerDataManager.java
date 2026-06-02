package dev.relicforging.data;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.RelicType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Responsible for loading, caching, and persisting player relic data.
 *
 * How persistence works:
 *   Each player gets their own YAML file at:
 *     plugins/RelicForging/playerdata/<uuid>.yml
 *
 *   This is a common, simple approach for Paper plugins because it avoids
 *   a full database dependency while still being safe across restarts.
 *   The trade-off is that it doesn't scale to thousands of simultaneous
 *   players without an async I/O layer, but for a typical survival/SMP
 *   server it performs well.
 *
 * The in-memory cache:
 *   While a player is online, their PlayerRelicData object lives in the
 *   `cache` map.  All ability, energy, and XP operations read/write that
 *   object directly — no disk I/O during gameplay.  The file is only
 *   touched on join (load) and quit (save).
 */
public class PlayerDataManager {

    private final RelicForgingPlugin plugin;
    private final File dataFolder;

    // The in-memory cache; the UUID is the player's unique Minecraft account ID.
    private final Map<UUID, PlayerRelicData> cache = new HashMap<>();

    public PlayerDataManager(RelicForgingPlugin plugin) {
        this.plugin     = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    // ----------------------------------------------------------------
    // Load / Save
    // ----------------------------------------------------------------

    /**
     * Loads (or creates) a PlayerRelicData for the given player and puts it
     * into the cache.  Should be called from a PlayerJoinEvent handler.
     */
    public PlayerRelicData loadPlayer(UUID uuid) {
        File file = playerFile(uuid);
        PlayerRelicData data = new PlayerRelicData(uuid);

        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Load equipped relic (may be absent for new players)
            String equippedStr = yaml.getString("equipped");
            if (equippedStr != null) {
                try {
                    RelicType type = RelicType.valueOf(equippedStr);
                    // We call the internal setter so energy starts at 0;
                    // the energy value is loaded separately below.
                    data.setEquippedType(type);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown relic type in save file for " + uuid + ": " + equippedStr);
                }
            }

            // Load stored energy (if any)
            data.setEnergy(yaml.getInt("energy", 0));

            // Load cooldowns
            if (yaml.isConfigurationSection("cooldowns")) {
                for (String key : yaml.getConfigurationSection("cooldowns").getKeys(false)) {
                    data.getCooldownExpiry().put(key, yaml.getLong("cooldowns."+key));
                }
            }

            // Load per-relic XP and levels
            for (RelicType type : RelicType.values()) {
                String key = type.configKey();
                data.getXpMap().put(type, yaml.getInt("progress." + key + ".xp", 0));
                data.getLevelMap().put(type, yaml.getInt("progress." + key + ".level", 1));
            }
        }

        cache.put(uuid, data);
        return data;
    }

    /**
     * Writes the player's current data to disk.
     * Should be called from a PlayerQuitEvent handler (and on server shutdown).
     */
    public void savePlayer(UUID uuid) {
        PlayerRelicData data = cache.get(uuid);
        if (data == null) return;

        YamlConfiguration yaml = new YamlConfiguration();

        if (data.isRelicEquipped()) {
            yaml.set("equipped", data.getEquippedType().name());
        }
        yaml.set("energy", data.getEnergy());

        for (Map.Entry<String, Long> entry : data.getCooldownExpiry().entrySet()) {
            yaml.set("cooldowns."+entry.getKey(), entry.getValue());
        }

        // Persist per-relic progression so leveling survives logouts
        for (RelicType type : RelicType.values()) {
            String key = type.configKey();
            yaml.set("progress." + key + ".xp",    data.getXpMap().getOrDefault(type, 0));
            yaml.set("progress." + key + ".level",  data.getLevelMap().getOrDefault(type, 1));
        }

        try {
            yaml.save(playerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data for player " + uuid, e);
        }
    }

    /** Saves all online players — called on plugin disable to prevent data loss. */
    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            savePlayer(uuid);
        }
    }

    // ----------------------------------------------------------------
    // Cache access
    // ----------------------------------------------------------------

    /** Returns the cached data for an online player, or null if not loaded. */
    public PlayerRelicData getData(UUID uuid) {
        return cache.get(uuid);
    }

    /** Removes a player from the in-memory cache after saving. */
    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /** Returns the YAML file for a player's data — also used by admin reset. */
    public File getPlayerFile(UUID uuid) {
        return playerFile(uuid);
    }

    private File playerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}
