package dev.relicforging.command;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Administrative commands for managing the relic system.
 *
 * Subcommands:
 *   /relicadmin give <player> <relic>  — hand a relic item to a player
 *   /relicadmin reset <player>         — wipe a player's relic data
 *   /relicadmin reload                 — hot-reload config.yml
 *
 * All subcommands require the relicforging.admin permission (op by default).
 */
public class RelicAdminCommand implements CommandExecutor, TabCompleter {

    private final RelicForgingPlugin plugin;

    public RelicAdminCommand(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("relicforging.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /relicadmin give <player> <relic>
            case "give": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /relicadmin give <player> <relic>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[1]);
                    return true;
                }
                RelicType type;
                try {
                    type = RelicType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cUnknown relic: " + args[2]
                        + ". Valid: " + Arrays.stream(RelicType.values())
                            .map(RelicType::configKey).collect(Collectors.joining(", ")));
                    return true;
                }
                Relic relic = plugin.getRelicManager().getRelic(type);
                if (relic == null) {
                    sender.sendMessage("§cRelic type is registered but has no behaviour — check startup errors.");
                    return true;
                }
                // Drop any overflow into their inventory; if full, drops on the ground.
                var leftover = target.getInventory().addItem(relic.createItem());
            leftover.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
                target.sendMessage("§aYou received a " + relic.getDisplayName() + "§a!");
                sender.sendMessage("§aGave " + relic.getDisplayName() + " §ato §f" + target.getName() + "§a.");
                break;
            }

            // /relicadmin reset <player>
            case "reset": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /relicadmin reset <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found (must be online): " + args[1]);
                    return true;
                }
                // 1. Cleanly remove passives and unload from cache (this also saves the current
                //    data — but we're about to delete the file anyway).
                plugin.getRelicManager().unequipRelic(target);
                plugin.getDataManager().unloadPlayer(target.getUniqueId());

                // 2. Delete the player's data file so loadPlayer() starts fresh.
                java.io.File dataFile = plugin.getDataManager().getPlayerFile(target.getUniqueId());
                if (dataFile.exists()) dataFile.delete();

                // 3. Reload — this creates a new blank PlayerRelicData and caches it.
                plugin.getDataManager().loadPlayer(target.getUniqueId());

                target.sendMessage("§eYour relic data has been reset by an administrator.");
                sender.sendMessage("§aReset relic data for §f" + target.getName() + "§a.");
                break;
            }

            // /relicadmin reload
            case "reload": {
                plugin.reloadConfig();
                plugin.getRelicManager().reloadConfig();
                sender.sendMessage("§aRelicForging config reloaded.");
                break;
            }

            default:
                sendAdminHelp(sender);
                break;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Tab completion
    // ----------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("relicforging.admin")) return List.of();

        if (args.length == 1) {
            return Arrays.asList("give", "reset", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            // Suggest online player names.
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Suggest relic names.
            return Arrays.stream(RelicType.values())
                .map(RelicType::configKey)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6=== RelicForging Admin Commands ===");
        sender.sendMessage("§e/relicadmin give <player> <relic> §7— Give a relic item");
        sender.sendMessage("§e/relicadmin reset <player>         §7— Wipe a player's relic data");
        sender.sendMessage("§e/relicadmin reload                 §7— Reload config.yml");
    }
}
