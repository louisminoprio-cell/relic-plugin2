package dev.relicforging.listener;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.manager.RelicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Detects when a player equips or removes a relic.
 *
 * How the equip slot works:
 *   We use Minecraft's built-in off-hand slot (F key) as the dedicated
 *   relic slot because it is the only single-item slot accessible to a
 *   Paper plugin without a resource pack custom GUI.  The off-hand is
 *   visible in first-person and third-person, and the swap-hand key
 *   already feels like "equipping" something special.
 *
 *   When the player puts a relic item in their off-hand (either by pressing
 *   F while holding it or by clicking it into the off-hand slot in the
 *   inventory screen) we call RelicManager.equipRelic().  When the off-hand
 *   is cleared of a relic, we call unequipRelic().
 */
public class RelicInventoryListener implements Listener {

    private final RelicForgingPlugin plugin;

    public RelicInventoryListener(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Swap Hand (F key) — the most natural way to equip a relic
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // After the swap, the new off-hand item is event.getOffHandItem().
        ItemStack newOffhand = event.getOffHandItem();
        RelicType newType    = RelicManager.getRelicTypeFromItem(newOffhand);

        if (newType != null) {
            // Player just swapped a relic into their off-hand.
            plugin.getRelicManager().equipRelic(player, newType);
        } else {
            // Player swapped something non-relic into their off-hand.
            // If they had a relic equipped, we must unequip it.
            PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
            if (data != null && data.isRelicEquipped()) {
                plugin.getRelicManager().unequipRelic(player);
            }
        }
    }

    // ----------------------------------------------------------------
    // Inventory Click — handles dragging relic into/out of off-hand slot
    // ----------------------------------------------------------------

    /**
     * Raw slot 40 in a player's inventory corresponds to the off-hand.
     * When a player drags a relic into slot 40, equip it.
     * When they drag it out, unequip.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        boolean clickedOffhand = (event.getRawSlot() == 40);
        if (!clickedOffhand) return;

        // Schedule a 1-tick delay so we read the off-hand state AFTER the
        // click has been processed by the server, rather than before.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack offhand  = player.getInventory().getItemInOffHand();
            RelicType newType  = RelicManager.getRelicTypeFromItem(offhand);
            PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());

            if (newType != null) {
                // A relic was placed in the off-hand.
                plugin.getRelicManager().equipRelic(player, newType);
            } else if (data != null && data.isRelicEquipped()) {
                // The off-hand is now empty (or non-relic) — unequip.
                plugin.getRelicManager().unequipRelic(player);
            }
        }, 1L);
    }
}
