
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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tide Relic — Water, Support, and Utility.
 */
public class TideRelic extends Relic {

    public TideRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.TIDE; }

    @Override
    public int getCustomModelData() { return 1003; }

    @Override
    protected Material getMaterial() {
        return Material.HEART_OF_THE_SEA;
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.WATER_BREATHING, 40, 0, true, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION, 40, 0, true, false, false
            ));

            if (Math.random() < 0.3) data.addXp(2);
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        double radius = 5.0;

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );

        for (Entity e : nearby) {
            Vector push = e.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .normalize()
                .multiply(1.4)
                .setY(0.5);
            e.setVelocity(push);

            if (e instanceof LivingEntity && !(e instanceof Player)) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 60, 1, false, true, true
                ));
            }
        }

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
            double x = Math.cos(angle) * radius * 0.6;
            double z = Math.sin(angle) * radius * 0.6;
            player.getWorld().spawnParticle(Particle.WATER_SPLASH,
                player.getLocation().add(x, 0.5, z), 3, 0, 0, 0, 0.1);
        }
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.4f);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.TIDE);
        double radius = (level >= 20) ? 12.0 : 8.0;

        Set<Player> allies = new LinkedHashSet<>();
        allies.add(player);

        Collection<Entity> nearbyPlayers = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof Player
        );

        for (Entity e : nearbyPlayers) {
            if (e instanceof Player ally && TeamCompatibility.isAllied(player, ally)) {
                allies.add(ally);
            }
        }

        for (Player ally : allies) {
            ally.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE, 100, 1, false, true, true
            ));
            ally.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION, 100, 1, false, true, true
            ));

            if (level >= 20 && ally.getFireTicks() > 0) {
                ally.setFireTicks(0);
            }

            if (level >= 30) {
                ally.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, 100, 0, false, true, true
                ));
            }

            ally.sendMessage("§3✦ Ocean Blessing washes over you!");
        }

        player.getWorld().spawnParticle(Particle.BUBBLE_POP,
            player.getLocation().add(0, 1, 0), 60, radius / 2, 1, radius / 2, 0.1);
        player.getWorld().playSound(player.getLocation(),
            Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);
    }
}
