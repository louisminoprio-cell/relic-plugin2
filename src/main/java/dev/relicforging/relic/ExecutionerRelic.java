package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData; 

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;

public class ExecutionerRelic extends Relic {

    public ExecutionerRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
    }

    @Override
    public int getCustomModelData() {
        return 101;
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 4);
        if (target == null) return;

        double damage = 4;

            player.getWorld().spawnParticle(
        org.bukkit.Particle.EXPLOSION,
        player.getLocation().add(0, 1, 0),
        15,
        0.4, 0.4, 0.4,
        0.1
    );

        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            damage += 3;
        }

        target.damage(damage, player);
    }

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 8);
        if (target == null) return;
       
            player.getWorld().spawnParticle(
       org.bukkit.Particle.DRIPPING_LAVA,
                player.getLocation().add(0,1,0),
                50,
                0.5, 0.5, 0.5,
                0.1
            );
        
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

    @Override
    public RelicType getType() {
        return RelicType.VANGUARD;
    }

    @Override
    public void removePassive(Player player) {
    }
}
