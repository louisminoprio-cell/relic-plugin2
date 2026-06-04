
package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ExecutionerRelic extends Relic {

    public ExecutionerRelic() {
        super("Executioner");
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 4);
        if (target == null) return;

        double damage = 4;

        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            damage += 3;
        }

        target.damage(damage, player);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 8);
        if (target == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid()) return;
            if (target.getLocation().distance(player.getLocation()) > 8) return;

            double damage = target.getHealth() > target.getMaxHealth() * 0.5 ? 8 : 12;
            target.damage(damage, player);

        }, 20); // 1 second
    }

    @Override
    public int getCustomModelData() {
        return 101; // change per relic
}

    // Passive handled in damage listener
}
