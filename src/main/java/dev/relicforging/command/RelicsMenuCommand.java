package dev.relicforging.command;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens a 54-slot chest GUI that shows the player's relic collection,
 * their progress with each relic, and a summary of what each one does.
 *
 * Why a chest GUI rather than a book?
 *   A chest GUI is visually richer and lets us display glowing item icons
 *   directly.  We don't allow taking items from this inventory — it is purely
 *   informational.  The actual relic items live in the player's inventory.
 *
 * The inventory is NOT persistent across sessions.  We rebuild it every time
 * the player opens it so they always see their latest XP and level data.
 */
public class RelicsMenuCommand implements CommandExecutor {

    private final RelicForgingPlugin plugin;

    // The title string is also the key used in RelicMenuListener to verify
    // that a click event is inside this GUI and not some other inventory.
    public static final String GUI_TITLE = "§6✦ Relic Collection";

    public RelicsMenuCommand(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can open the relic menu.");
            return true;
        }
        Player player = (Player) sender;
        openMenu(player);
        return true;
    }

    /**
     * Builds and opens the relic collection GUI for the given player.
     * This is also called by the RelicMenuListener when the player clicks
     * "back" from a detail screen in a future update.
     */
    public void openMenu(Player player) {
        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill the border with dark glass panes to frame the content cleanly.
        ItemStack border = makeBorderPane();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 54; i += 9) inv.setItem(i, border);

        // Place each relic in the centre rows, 3 relics per row.
        RelicType[] types = RelicType.values();
        int[] slots = {10, 12, 14, 28, 30, 32};     // 6 slots in a 3-2 grid pattern

        for (int i = 0; i < types.length && i < slots.length; i++) {
            RelicType type = types[i];
            Relic relic    = plugin.getRelicManager().getRelic(type);
            if (relic == null) continue;

            ItemStack display = buildRelicDisplayItem(relic, data, type);
            inv.setItem(slots[i], display);
        }

        // A "currently equipped" indicator in the bottom row.
        if (data != null && data.isRelicEquipped()) {
            Relic equipped = plugin.getRelicManager().getRelic(data.getEquippedType());
            if (equipped != null) {
                ItemStack indicator = makeEquippedIndicator(equipped);
                inv.setItem(49, indicator);
            }
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    /**
     * Creates the display item for a single relic inside the menu.
     * We clone the relic's own item and append current level/XP data to the lore.
     */
    private ItemStack buildRelicDisplayItem(Relic relic, PlayerRelicData data, RelicType type) {
        ItemStack item = relic.createItem();
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        lore.add("");

        if (data != null) {
            int level  = data.getLevel(type);
            int xp     = data.getXp(type);
            int needed = PlayerRelicData.xpForLevel(level + 1);
            String tier = (level >= 21 ? "III" : level >= 11 ? "II" : "I");

            lore.add("§7Level §f" + level + " §8(Tier " + tier + ")");
            if (level < 30) {
                lore.add("§7XP: §e" + xp + "§7/§e" + needed);
            } else {
                lore.add("§aMax Level!");
            }

            if (data.isRelicEquipped() && data.getEquippedType() == type) {
                lore.add("");
                lore.add("§a► Currently Equipped");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeBorderPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta  = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack makeEquippedIndicator(Relic relic) {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aEquipped: " + relic.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Use §e/1 §7and §e/2 §7to activate abilities.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
