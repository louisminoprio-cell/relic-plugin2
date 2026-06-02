
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

public class EmberRelic extends Relic {

    public EmberRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.EMBER; }

    @Override
    public int getCustomModelData() { return 1002; }

    @Override
    protected Material getMaterial() {
        return Material.BLAZE_ROD;
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        if (Math.random() < 0.15) {
            player.getWorld().spawnParticle(Particle.SMALL_FLAME,
                player.getLocation().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        int level  = data.getLevel(RelicType.EMBER);
        double radius = (level >= 10) ? 5.0 : 4.0;

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );

        for (Entity e : nearby) {
            LivingEntity le = (LivingEntity) e;
            le.setFireTicks(80);
            le.damage(3.0, player);
        }

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.FIRE_RESISTANCE, 60, 0, false, true, true
        ));

        player.getWorld().spawnParticle(Particle.FLAME,
            player.getLocation().add(0, 1, 0), 40, radius / 2, 0.5, radius / 2, 0.08);
        player.getWorld().spawnParticle(Particle.LAVA,
            player.getLocation(), 15, radius / 3, 0.5, radius / 3, 0);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        int    level  = data.getLevel(RelicType.EMBER);
        double range  = 7.0;
        double cosAngle = Math.cos(Math.toRadians(45));

        Vector look = player.getLocation().getDirection().normalize();

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), range, range, range,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );

        for (Entity e : nearby) {
            Vector toEntity = e.getLocation().toVector()
                .subtract(player.getLocation().toVector()).normalize();
            if (toEntity.dot(look) >= cosAngle) {
                LivingEntity le = (LivingEntity) e;
                le.setFireTicks(120);
                le.damage(5.5, player);
                le.setVelocity(look.clone().multiply(0.6).setY(0.3));
            }
        }

        if (level >= 30) {
            Location loc = player.getLocation().clone();
            for (int i = 1; i <= 5; i++) {
                Location trail = loc.clone().add(look.clone().multiply(i));
                trail.getWorld().spawnParticle(Particle.FLAME,
                    trail.add(0.5, 0.1, 0.5), 8, 0.2, 0.1, 0.2, 0.02);
            }
        }

        for (int step = 1; step <= 7; step++) {
            Location point = player.getLocation().clone().add(look.clone().multiply(step));
            player.getWorld().spawnParticle(Particle.FLAME, point, 8, 0.4, 0.3, 0.4, 0.05);
        }
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.6f);
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
    }
}
