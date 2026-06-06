package dev.relicforging;

import dev.relicforging.api.RelicKeys;
import dev.relicforging.command.*;
import dev.relicforging.data.PlayerDataManager;
import dev.relicforging.listener.*;
import dev.relicforging.manager.RelicManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * RelicForgingPlugin — the entry point for the entire plugin.
 *
 * This class is intentionally kept thin: it just wires together the three
 * core subsystems (RelicManager, PlayerDataManager, and the command/listener
 * layer) and provides getter access to them.  All real logic lives in the
 * subsystem classes so this file stays easy to read at a glance.
 *
 * Startup order matters here:
 *   1. Save default config so server admins get the config.yml immediately.
 *   2. Initialise NamespacedKeys (RelicKeys.init) — must happen before any
 *      item or NBT operations, which means before the managers start.
 *   3. Create PlayerDataManager (stateless until players join).
 *   4. Create and init RelicManager — loads config and starts tick tasks.
 *   5. Register commands and listeners.
 *
 * Shutdown order:
 *   1. Cancel RelicManager tasks so they don't fire on a partially-unloaded server.
 *   2. Save all online player data so no one loses progress on /reload or restart.
 */
public class RelicForgingPlugin extends JavaPlugin {

    // The two core subsystems — exposed via getters so commands and listeners
    // can reach them without singletons or static state.
    private RelicManager      relicManager;
    private PlayerDataManager dataManager;
    private SoulboundRelic soulboundRelic;

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @Override
    public void onEnable() {
        // Step 1: write config.yml to disk if it doesn't exist yet.
        saveDefaultConfig();

        soulboundRelic = new SoulboundRelic(this);

        // Step 2: initialise all NamespacedKeys with a reference to this plugin.
        //         Must happen before RelicManager tries to stamp relic items with NBT.
        RelicKeys.init(this);

        // Step 3: create the data manager (no I/O yet — players aren't online).
        dataManager  = new PlayerDataManager(this);

        // Step 4: create and start the relic manager.
        relicManager = new RelicManager(this);
        relicManager.init();

        // Step 5: register commands.
        // AbilityCommand is shared — both /1 and /2 use it with a different flag.
        getCommand("1").setExecutor(new AbilityCommand(this, true));
        getCommand("2").setExecutor(new AbilityCommand(this, false));

        RelicCommand     relicCmd = new RelicCommand(this);
        RelicsMenuCommand menuCmd = new RelicsMenuCommand(this);
        RelicAdminCommand adminCmd = new RelicAdminCommand(this);

        getCommand("relic").setExecutor(relicCmd);
        getCommand("relic").setTabCompleter(relicCmd);
        getCommand("relics").setExecutor(menuCmd);
        getCommand("relicadmin").setExecutor(adminCmd);
        getCommand("relicadmin").setTabCompleter(adminCmd);

        // Step 6: register event listeners.
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RelicInventoryListener(this),   this);
        getServer().getPluginManager().registerEvents(new RelicDamageListener(this),      this);
        getServer().getPluginManager().registerEvents(new RelicMechanicsListener(this),   this);
        getServer().getPluginManager().registerEvents(new RelicMenuListener(),            this);

        getLogger().info("RelicForging enabled successfully!");
        getLogger().info("Run /relicadmin give <player> <relic> to hand out your first relic.");

        
        public SoulboundRelic getSoulboundRelic() {
            return soulboundRelic;
}
    }

    @Override
    public void onDisable() {
        // Stop tick tasks first so they don't read from a partially-shut-down state.
        if (relicManager != null) {
            relicManager.shutdown();
        }
        // Save every online player's data so no progress is lost on /reload or restart.
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("RelicForging disabled. All player data saved.");
    }

    // ----------------------------------------------------------------
    // Getters — used by commands, listeners, and relic classes
    // ----------------------------------------------------------------

    public RelicManager      getRelicManager() { return relicManager; }
    public PlayerDataManager getDataManager()  { return dataManager;  }
}
