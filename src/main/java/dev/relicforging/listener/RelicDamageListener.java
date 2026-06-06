
package dev.relicforging.listener;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.relic.SoulboundRelic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.Bukkit;

/**
 * Intercepts damage events to apply relic-specific passive damage modifiers.
 */
public class RelicDamageListener implements Listener {

    private final RelicForgingPlugin plugin;

    public RelicDamageListener(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSoulLinkDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
    
        SoulboundRelic relic = plugin.getSoulboundRelic();
    
        if (!relic.isLinked(victim)) return;
    
        // ❗ Prevent infinite loop
        if (relic.processing.contains(victim)) return;

        Player partner = relic.getLinked(victim);
        if (partner == null || !partner.isOnline()) return;
    
        double shared = event.getDamage() * 0.3;

        // Reduce original damage
        event.setDamage(event.getDamage() * 0.7);
    
        // Mark both as processing
        relic.processing.add(victim);
        relic.processing.add(partner);

        // Apply shared damage
        partner.damage(shared);

        // Remove processing after tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
        relic.processing.remove(victim);
        relic.processing.remove(partner);
        }, 1L);
    }


    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            attacker = player;
        }

        if (attacker == null) {
            return;
        }

        if (TeamCompatibility.areTeammates(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Generic entity damage — fall damage (Gale) and projectile (Vanguard)
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerRelicData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || !data.isRelicEquipped()) return;

        RelicType equipped = data.getEquippedType();

        // ================= SOULBOUND DAMAGE SHARE =================
        if (equipped == RelicType.SOULBOUND) {

            Player partner = plugin.getRelicManager().getSoulboundPartner(player);

            if (partner != null && partner.isOnline()) {

                double damage = event.getDamage();
                double shared = damage * 0.3;

                // reduce original damage
                event.setDamage(damage * 0.7);

                // apply shared damage safely
                partner.damage(shared);

                // ✨ particles between players
                player.getWorld().spawnParticle(
                        org.bukkit.Particle.SOUL_FIRE_FLAME,
                        player.getLocation().add(0, 1, 0),
                        10
                );

                partner.getWorld().spawnParticle(
                        org.bukkit.Particle.SOUL_FIRE_FLAME,
                        partner.getLocation().add(0, 1, 0),
                        10
                );

                // 🔊 sound feedback
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_WITHER_HURT, 1f, 1.2f);

                partner.playSound(partner.getLocation(),
                        org.bukkit.Sound.ENTITY_WITHER_HURT, 1f, 1.2f);
            }
        }

        if (equipped == RelicType.GALE
            && event.getCause() == EntityDamageEvent.DamageCause.FALL) {

            int level = data.getLevel(RelicType.GALE);
            if (level >= 10) {
                event.setCancelled(true);
            } else {
                event.setDamage(event.getDamage() * 0.30);
            }
        }

        if (equipped == RelicType.VANGUARD
            && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setDamage(event.getDamage() * 0.70);
        }
    }

    // ----------------------------------------------------------------
    // Player attacking entity — Ember bonus damage vs burning mobs
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        PlayerRelicData data = plugin.getDataManager().getData(attacker.getUniqueId());
        if (data == null || !data.isRelicEquipped()) return;

        if (event.getEntity() instanceof Player victim && TeamCompatibility.areTeammates(attacker, victim)) {
            return;
        }

         //if (isSoulLinked(victim)) {
           // Player partner = getLinkedPlayer(victim);
           // double shared = event.getDamage() * 0.3;

           // event.setDamage(event.getDamage() * 0.7);
           // partner.damage(shared);
       // }

        //if (data.getEquippedType() == RelicType.EMBER
           // && data.getLevel(RelicType.EMBER) >= 20
            //&& event.getEntity() instanceof LivingEntity
            //&& event.getEntity().getFireTicks() > 0) {

           // event.setDamage(event.getDamage() * 1.20);
        //}
    }
}
