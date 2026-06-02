package dev.relicforging.api;

import dev.relicforging.RelicForgingPlugin;
import org.bukkit.NamespacedKey;

/**
 * Central registry for every NamespacedKey used in PersistentDataContainers.
 *
 * Why a dedicated class?
 *   NamespacedKeys are used to embed custom data inside ItemMeta and Entity
 *   metadata that survives server restarts.  Keeping them all here prevents
 *   typo bugs and makes it easy to audit what data the plugin writes to items.
 *
 * All keys are lazily initialised on first access via init(plugin).
 */
public final class RelicKeys {

    // Stored on every relic ItemStack — identifies which RelicType it is.
    public static NamespacedKey RELIC_TYPE;

    // Stored on relic items to track their level visually in lore updates.
    public static NamespacedKey RELIC_LEVEL;

    private RelicKeys() { /* static utility class, no instances */ }

    /**
     * Must be called once during plugin enable, before any item creation.
     *
     * @param plugin the plugin instance (required to construct NamespacedKeys)
     */
    public static void init(RelicForgingPlugin plugin) {
        RELIC_TYPE  = new NamespacedKey(plugin, "relic_type");
        RELIC_LEVEL = new NamespacedKey(plugin, "relic_level");
    }
}
