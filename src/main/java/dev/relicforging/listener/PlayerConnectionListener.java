package dev.relicforging.listener;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.manager.RelicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Manages the player data lifecycle: load on join, save on quit.
 *
 * We also re-sync the relic equip state here because a player might log out
 * with a relic in their off-hand. When they log back in we detect that relic
 * in their off-hand and re-apply passives so there's no "dead period" where
 * they're carrying a relic but not benefiting from it.
 */
public class PlayerConnectionListener implements Listener {

    private final RelicForgingPlugin plugin;

    public PlayerConnectionListener(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load from disk into the in-memory cache.
        PlayerRelicData data = plugin.getDataManager().loadPlayer(player.getUniqueId());

        // Check the off-hand for a relic in case the player logged out with one equipped.
        // This re-synchronises the equipped state so passives fire from tick 1.
        ItemStack offhand = player.getInventory().getItemInOffHand();
        RelicType typeFromItem = RelicManager.getRelicTypeFromItem(offhand);

        if (typeFromItem != null) {
            // The off-hand contains a relic — make sure it's registered as equipped.
            if (!data.isRelicEquipped() || data.getEquippedType() != typeFromItem) {
                plugin.getRelicManager().equipRelic(player, typeFromItem);
            }
        } else if (data.isRelicEquipped()) {
            // The saved data says equipped, but the off-hand is empty — clear it.
            // This prevents ghost-passives if the relic was somehow removed while offline.
            data.clearEquipped();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Remove passives cleanly so they don't persist on the entity after logout.
        plugin.getRelicManager().unequipRelic(player);
        // Save to disk and remove from cache to prevent memory leaks.
        plugin.getDataManager().unloadPlayer(player.getUniqueId());
    }
}
