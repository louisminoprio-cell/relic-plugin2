package dev.relicforging;

import dev.relicforging.api.RelicKeys;
import dev.relicforging.command.*;
import dev.relicforging.data.PlayerDataManager;
import dev.relicforging.listener.*;
import dev.relicforging.manager.RelicManager;
import org.bukkit.plugin.java.JavaPlugin;


public class RelicForgingPlugin extends JavaPlugin {

    
    private RelicManager      relicManager;
    private PlayerDataManager dataManager;
    private SoulboundRelic soulboundRelic;

    @Override
    public void onEnable() {
        
        saveDefaultConfig();

        soulboundRelic = new SoulboundRelic(this);

     
        RelicKeys.init(this);

    
        dataManager  = new PlayerDataManager(this);

        
        relicManager = new RelicManager(this);
        relicManager.init();

        
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

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RelicInventoryListener(this),   this);
        getServer().getPluginManager().registerEvents(new RelicDamageListener(this),      this);
        getServer().getPluginManager().registerEvents(new RelicMechanicsListener(this),   this);
        getServer().getPluginManager().registerEvents(new RelicMenuListener(),            this);

        getLogger().info("RelicForging enabled successfully!");
        getLogger().info("Run /relicadmin give <player> <relic> to hand out your first relic.");

    }

    @Override
    public void onDisable() {

        if (relicManager != null) {
            relicManager.shutdown();
        }
   
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("RelicForging disabled. All player data saved.");
    }

    public SoulboundRelic getSoulboundRelic() {
        return soulboundRelic;
    }

    public RelicManager      getRelicManager() { return relicManager; }
    public PlayerDataManager getDataManager()  { return dataManager;  }
}
