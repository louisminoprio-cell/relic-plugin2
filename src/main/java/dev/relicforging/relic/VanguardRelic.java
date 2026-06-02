
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
 * Vanguard Relic — Tank, Protection, and Support.
 */
public class VanguardRelic extends Relic {

    public VanguardRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.VANGUARD; }

    @Override
    public int getCustomModelData() { return 1005; }

    @Override
    protected Material getMaterial() {
        return Material.IRON_CHESTPLATE;
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, 40, 0, true, false, false
        ));
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.VANGUARD);
        int absorptionTier = (level >= 10) ? 1 : 0;
        double radius = (level >= 30) ? 15.0 : 8.0;

        Set<Player> allies = new LinkedHashSet<>();
        allies.add(player);

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof Player && !e.equals(player)
        );

        for (Entity e : nearby) {
            if (e instanceof Player ally && TeamCompatibility.isAllied(player, ally)) {
                allies.add(ally);
            }
        }

        for (Player ally : allies) {
            ally.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION, 200, absorptionTier, false, true, true
            ));
            ally.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, 100, 1, false, true, true
            ));
            ally.sendMessage("§7✦ Fortified!");
        }

        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
            player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(),
            Sound.ITEM_SHIELD_BLOCK, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 1.5f);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.VANGUARD);
        double radius = 5.0;

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );

        for (Entity e : nearby) {
            LivingEntity le = (LivingEntity) e;

            Vector knock = le.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .normalize()
                .multiply(1.6)
                .setY(0.4);
            le.setVelocity(knock);
            le.damage(2.5, player);

            le.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 60, 1, false, true, true
            ));

            if (level >= 20) {
                le.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA, 40, 0, false, true, true
                ));
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION,
            player.getLocation(), 3, 0.3, 0, 0.3, 0);
        player.getWorld().spawnParticle(Particle.CRIT,
            player.getLocation().add(0, 1, 0), 30, 1.5, 0.5, 1.5, 0.1);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
    }
}
