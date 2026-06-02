package dev.relicforging.command;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * The /relic command family: xp, info, energy, help.
 *
 * These are the informational commands that let players inspect their
 * relic's current state.  They're intentionally read-only — actual ability
 * firing lives in AbilityCommand (/1 and /2).
 */
public class RelicCommand implements CommandExecutor, TabCompleter {

    private final RelicForgingPlugin plugin;

    public RelicCommand(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cYour relic data is not loaded.");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "xp":
            case "level": {
                // Show XP progress for the currently equipped relic (or all relics).
                if (!data.isRelicEquipped()) {
                    // Show a summary table across all relics.
                    player.sendMessage("§6--- Relic Mastery Progress ---");
                    for (RelicType type : RelicType.values()) {
                        int level  = data.getLevel(type);
                        int xp     = data.getXp(type);
                        int needed = PlayerRelicData.xpForLevel(level + 1);
                        player.sendMessage("  §e" + typeName(type) + "§7 — §fLvl " + level
                            + " §7(" + xp + "/" + (level >= 30 ? "MAX" : needed) + " XP)");
                    }
                } else {
                    RelicType type = data.getEquippedType();
                    int level  = data.getLevel(type);
                    int xp     = data.getXp(type);
                    int needed = PlayerRelicData.xpForLevel(level + 1);
                    Relic relic = plugin.getRelicManager().getRelic(type);

                    player.sendMessage("§6--- " + (relic != null ? relic.getDisplayName() : typeName(type)) + " §6Mastery ---");
                    player.sendMessage("  §fLevel: §e" + level + " §8(Tier " + tier(level) + ")");
                    if (level < 30) {
                        int percent = (int) ((xp * 100.0) / needed);
                        player.sendMessage("  §fXP: §e" + xp + "§7/§e" + needed + " §8(" + percent + "%)");
                        // A simple ASCII progress bar.
                        player.sendMessage("  " + progressBar(xp, needed, 20));
                    } else {
                        player.sendMessage("  §aMaximum level reached!");
                    }
                }
                break;
            }

            case "info": {
                if (!data.isRelicEquipped()) {
                    player.sendMessage("§7No relic equipped. Put a relic in your off-hand.");
                    break;
                }
                RelicType type  = data.getEquippedType();
                Relic relic     = plugin.getRelicManager().getRelic(type);
                int level       = data.getLevel(type);
                int energy      = data.getEnergy();
                int maxEnergy   = relic != null ? relic.getMaxEnergy() : 0;
                int pCd         = data.getCooldownSeconds("primary");
                int sCd         = data.getCooldownSeconds("secondary");

                player.sendMessage("§6--- " + (relic != null ? relic.getDisplayName() : typeName(type)) + " §6Info ---");
                player.sendMessage("  §fLevel: §e" + level + " §8(Tier " + tier(level) + ")");
                player.sendMessage("  §fEnergy: §e" + energy + "§7/§e" + maxEnergy);
                if (relic != null) {
                    player.sendMessage("  §e/1 §f" + relic.getPrimaryName()
                        + ": " + (pCd > 0 ? "§c" + pCd + "s cooldown" : "§aready"));
                    player.sendMessage("  §e/2 §f" + relic.getSecondaryName()
                        + ": " + (sCd > 0 ? "§c" + sCd + "s cooldown" : "§aready"));
                }
                break;
            }

            case "energy": {
                if (!data.isRelicEquipped()) {
                    player.sendMessage("§7No relic equipped.");
                    break;
                }
                Relic relic = plugin.getRelicManager().getRelic(data.getEquippedType());
                int max = relic != null ? relic.getMaxEnergy() : 100;
                player.sendMessage("§b⚡ Energy: §f" + data.getEnergy() + "§7/§f" + max
                    + "  " + progressBar(data.getEnergy(), max, 15));
                break;
            }

            case "help":
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Tab completion
    // ----------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("xp", "info", "energy", "help");
        }
        return List.of();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void sendHelp(Player player) {
        player.sendMessage("§6=== RelicForging Commands ===");
        player.sendMessage("§e/1 §7— Use primary relic ability");
        player.sendMessage("§e/2 §7— Use secondary relic ability");
        player.sendMessage("§e/relic info §7— View equipped relic status");
        player.sendMessage("§e/relic xp §7— View mastery progress");
        player.sendMessage("§e/relic energy §7— Check current energy");
        player.sendMessage("§e/relics §7— Open the relic menu");
        player.sendMessage("§7Equip a relic by holding it and pressing §eF§7.");
    }

    private String tier(int level) {
        if (level >= 21) return "III";
        if (level >= 11) return "II";
        return "I";
    }

    private String typeName(RelicType type) {
        // Capitalise first letter only.
        String n = type.name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    private String progressBar(int current, int max, int bars) {
        int filled = (int) ((current * (double) bars) / max);
        filled = Math.min(filled, bars);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a|" : "§7|");
        }
        sb.append("§8]");
        return sb.toString();
    }
}
