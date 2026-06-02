package dev.relicforging.command;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.AbilityResult;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.manager.RelicManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles both the /1 (primary) and /2 (secondary) ability commands.
 *
 * A single class handles both because the logic is identical except for the
 * `primary` boolean flag — no point duplicating 50 lines of code.  In plugin.yml
 * both commands point to this executor.
 *
 * Error messaging philosophy:
 *   Players should never feel like the command "did nothing".  Every failure path
 *   sends a clear, concise message so the player knows *why* their ability didn't
 *   fire and what they can do about it.
 */
public class AbilityCommand implements CommandExecutor {

    private final RelicForgingPlugin plugin;
    private final boolean primary;          // true = /1, false = /2

    public AbilityCommand(RelicForgingPlugin plugin, boolean primary) {
        this.plugin  = plugin;
        this.primary = primary;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Abilities can only be used by players (not the console).
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use relic abilities.");
            return true;
        }
        Player player = (Player) sender;

        // Retrieve the player's live data from the in-memory cache.
        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cYour relic data is not loaded. Try re-logging.");
            return true;
        }

        // Guard: no relic equipped.
        if (!data.isRelicEquipped()) {
            player.sendMessage("§7Hold a Relic in your off-hand to equip it, then use §e/1 §7and §e/2§7.");
            return true;
        }

        // Retrieve the behaviour object for the equipped relic.
        RelicType type = data.getEquippedType();
        Relic relic = plugin.getRelicManager().getRelic(type);
        if (relic == null) {
            player.sendMessage("§cUnknown relic type. Please report this to an admin.");
            return true;
        }

        // Delegate to the unified ability dispatcher in Relic.
        AbilityResult result = relic.useAbility(player, data, primary);

        // Give the player precise feedback for every non-success case.
        switch (result) {
            case SUCCESS:
                // Action bar is already updated by the energy regen task; no extra message needed.
                break;

            case ON_COOLDOWN: {
                String key = primary ? "primary" : "secondary";
                int remaining = data.getCooldownSeconds(key);
                String abilityName = primary ? relic.getPrimaryName() : relic.getSecondaryName();
                player.sendMessage("§c⏱ " + abilityName + " is cooling down — ready in §e" + remaining + "s§c.");
                break;
            }

            case NOT_ENOUGH_ENERGY: {
                String abilityName = primary ? relic.getPrimaryName() : relic.getSecondaryName();
                player.sendMessage("§c⚡ Not enough energy for " + abilityName + "!  "
                    + "§7(§f" + data.getEnergy() + "§7/§f" + relic.getMaxEnergy() + "§7)");
                break;
            }

            case CONDITIONS_NOT_MET:
                player.sendMessage("§cThe conditions for that ability aren't met right now.");
                break;

            case ERROR:
                player.sendMessage("§cSomething went wrong. Please report this to an admin.");
                break;

            default:
                break;
        }
        return true;
    }
}
