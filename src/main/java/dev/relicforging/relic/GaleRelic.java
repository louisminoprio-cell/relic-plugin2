
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

public class GaleRelic extends Relic {

    public GaleRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.GALE; }

    @Override
    public int getCustomModelData() { return 1001; }

    @Override
    protected org.bukkit.Material getMaterial() {
        return Material.FEATHER;
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        if (player.isSprinting()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 40, 0, true, false, false
            ));
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.GALE);
        double speed = 1.4 + (level / 10) * 0.15;

        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize().multiply(speed);
        dir.setY(0.35);

        player.setVelocity(dir);

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), 2.5, 2.5, 2.5,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );
        for (Entity e : nearby) {
            LivingEntity le = (LivingEntity) e;
            le.damage(2.0, player);
            Vector knock = le.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .normalize().multiply(0.8).setY(0.4);
            le.setVelocity(knock);
        }

        if (level >= 20) {
            data.setCooldown("primary", primaryCooldownTicks / 2);
        }

        player.getWorld().spawnParticle(PParticle.SWEEP_ATTACK,
            player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_BREEZE_WIND_BURST, 0.9f, 1.2f);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.GALE);
        double lift = 1.1 + (level / 10) * 0.1;

        Vector vel = player.getVelocity().clone();
        vel.setY(lift);
        player.setVelocity(vel);

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOW_FALLING, 60, 0, false, true, true
        ));

        player.getWorld().spawnParticle(Particle.CLOUD,
            player.getLocation(), 12, 0.2, 0.1, 0.2, 0.05);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
    }
}
