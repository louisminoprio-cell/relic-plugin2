package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.api.RelicType;

import org.bukkit.entity.Player;

public class SoulboundRelic extends Relic {

    public SoulboundRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public int getCustomModelData() {
        return 102;
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {
        Player ally = getNearbyPlayer(player, 6);
        if (ally == null) return;

        double amount = 3;

        if (player.getHealth() <= 1) return;

        player.setHealth(Math.max(1, player.getHealth() - amount));
        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + amount));
    }

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {
        Player ally = getNearbyPlayer(player, 8);
        if (ally == null) return;

        player.sendMessage("§aSoul Link activated with " + ally.getName());
    }

    private Player getNearbyPlayer(Player player, double range) {
        for (Player p : player.getWorld().getPlayers()) {
            if (p != player && p.getLocation().distance(player.getLocation()) <= range) {
                return p;
            }
        }
        return null;
    }
    @Override
    public RelicType getType() {
        return RelicType.VANGUARD;
    }
    @Override
    public void removePassive(Player player) {
    }
}
