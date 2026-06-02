package dev.relicforging.listener;

import dev.relicforging.command.RelicsMenuCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Prevents the player from removing items from the /relics chest GUI.
 *
 * Paper fires InventoryClickEvent and InventoryDragEvent whenever the player
 * interacts with any open inventory.  We check whether the title matches our
 * GUI title constant and, if so, cancel the event so no items can be stolen
 * from the display.
 *
 * The title check is a lightweight way to identify "our" inventory without
 * needing to store a reference to every open inventory instance.
 */
public class RelicMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(RelicsMenuCommand.GUI_TITLE)) {
            // Cancel ALL clicks inside this GUI to prevent item theft.
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(RelicsMenuCommand.GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
}
