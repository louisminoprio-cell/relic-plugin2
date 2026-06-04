package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;

public class ExecutionerRelic extends Relic {

    public ExecutionerRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() {
        return RelicType.EXECUTIONER; // make sure this exists
    }

    @Override
    public int getCustomModelData() {
        return 101;
    }

    @Override
    protected void doPrimary(Player player) {
        LivingEntity target = getTarget(player, 4);
        if (target == null) return;

        double damage = 4;

        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            damage += 3;
        }

        target.damage(damage, player);
    }

    @Override
    protected void doSecondary(Player player) {
        LivingEntity target = getTarget(player, 8);
        if (target == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid()) return;

            double damage = target.getHealth() > target.getMaxHealth() * 0.5 ? 8 : 12;
            target.damage(damage, player);

        }, 20);
    }

    private LivingEntity getTarget(Player player, double range) {
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && e != player) {
                return (LivingEntity) e;
            }
        }
        return null;
    }
}
