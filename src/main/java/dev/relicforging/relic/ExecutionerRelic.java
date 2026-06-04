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
        super("Executioner", RelicType.OFFENSIVE);
    }

    @Override
    public void onDamage(Player attacker, LivingEntity target, double[] damage) {
        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            damage[0] *= 1.2; // passive
        }
    }

    @Override
    public AbilityResult ability1(Player player) {
        LivingEntity target = getTarget(player, 4);
        if (target == null) return AbilityResult.FAIL;

        double dmg = 4;
        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            dmg += 3;
        }

        target.damage(dmg, player);
        return AbilityResult.SUCCESS;
    }

    @Override
    public AbilityResult ability2(Player player) {
        LivingEntity target = getTarget(player, 8);
        if (target == null) return AbilityResult.FAIL;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid() || target.getLocation().distance(player.getLocation()) > 8) return;

            double dmg = target.getHealth() > target.getMaxHealth() * 0.5 ? 8 : 12;
            target.damage(dmg, player);

        }, 20);

        return AbilityResult.SUCCESS;
    }
}
