package dev.relicforging.listener;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.integration.TeamCompatibility;
import dev.relicforging.relic.SoulboundRelic;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class RelicDamageListener implements Listener {

    private final RelicForgingPlugin plugin;

    public RelicDamageListener(RelicForgingPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------
    // Prevent team damage
    // -------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        if (TeamCompatibility.areTeammates(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    // -------------------------------
    // Soulbound damage sharing
    // -------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSoulLinkDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) return;

        SoulboundRelic relic = plugin.getSoulboundRelic();
        Player partner = relic.getLinkedPlayer(victim);

        if (partner == null || !partner.isOnline()) return;

        double originalDamage = event.getDamage();
        double sharedDamage = originalDamage * 0.3;

        // Reduce victim damage
        event.setDamage(originalDamage * 0.7);

        // Apply damage to partner safely (no recursion)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!partner.isDead()) {
                partner.damage(sharedDamage);
            }
        });
    }
}
