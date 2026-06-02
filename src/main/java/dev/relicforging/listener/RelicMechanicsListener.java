
package dev.relicforging.listener;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class RelicMechanicsListener implements Listener {
    private final RelicForgingPlugin plugin;
    private final Random random = new Random();

    public RelicMechanicsListener(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player victim) {
            Player attacker = null;
            if (e.getDamager() instanceof Player p) {
                attacker = p;
            } else if (e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null && TeamCompatibility.areTeammates(attacker, victim)) {
                return;
            }

            if (attacker != null) {
                PlayerRelicData data = plugin.getDataManager().getData(victim.getUniqueId());
                if (data != null && data.getEquippedType() == RelicType.PLAGUE) {
                    if (e.getDamager() instanceof LivingEntity le && random.nextDouble() <= 0.2) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                    }
                }
            }
        }

        if (e.getDamager() instanceof Player attacker) {
            PlayerRelicData data = plugin.getDataManager().getData(attacker.getUniqueId());
            if (data != null && data.getEquippedType() == RelicType.PLAGUE && data.getLevel(RelicType.PLAGUE) >= 20) {
                if (e.getEntity() instanceof Player victim && TeamCompatibility.areTeammates(attacker, victim)) {
                    return;
                }

                if (e.getEntity() instanceof LivingEntity le) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                }
            }
        }
    }

    @EventHandler
    public void onProjectile(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof LivingEntity)) return;
        Projectile proj = e.getEntity();

        for (Player p : proj.getWorld().getPlayers()) {
            PlayerRelicData data = plugin.getDataManager().getData(p.getUniqueId());
            if (data == null || data.getEquippedType() != RelicType.HOLLOW) continue;
            if (p.getLocation().distanceSquared(proj.getLocation()) > 25) continue;

            double chance = data.getLevel(RelicType.HOLLOW) >= 20 ? 0.45 : 0.25;
            if (random.nextDouble() <= chance) {
                Vector v = proj.getVelocity();
                proj.setVelocity(v.add(new Vector(
                        (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.5
                )));
                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, proj.getLocation(), 8, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @EventHandler
    public void onAbility(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if ((e.getMessage().equalsIgnoreCase("/1") || e.getMessage().equalsIgnoreCase("/2")) && p.hasMetadata("dirged")) {
            e.setCancelled(true);

            p.damage(p.hasMetadata("dirged_lvl30") ? 8 : 4);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10));

            p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation(), 2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.removeMetadata("dirged", plugin);
                p.removeMetadata("dirged_lvl30", plugin);
            }, 1L);
        }
    }
}
