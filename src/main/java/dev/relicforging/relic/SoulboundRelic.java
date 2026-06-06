package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound; // ✅ ADDED: sound support
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulboundRelic extends Relic {

    private final Map<Player, Player> links = new HashMap<>();
    private final Set<Player> processing = new HashSet<>();


    public void linkPlayers(Player a, Player b) {
        links.put(a, b);
        links.put(b, a);
    }

    public void unlink(Player p) {
        Player other = links.remove(p);
        if (other != null) {
            links.remove(other);
        }
    }

    public Player getLinked(Player p) {
        return links.get(p);
    }

    public boolean isLinked(Player p) {
        return links.containsKey(p);
    }    

    // ✅ ADDED: stores linked players
    private final Map<UUID, UUID> links = new HashMap<>();

    public SoulboundRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() {
        return RelicType.SOULBOUND;
    }

    @Override
    public int getCustomModelData() {
        return 1001; // you can change this number later
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        // No passive for now
    }

    @Override
    public void removePassive(Player player) {
        links.remove(player.getUniqueId());
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {

        // ✅ Find nearest player
        Player target = getNearestPlayer(player);

        if (target == null) {
            player.sendMessage("§cNo nearby player to link!");
            return;
        }


             player.getWorld().spawnParticle(
        org.bukkit.Particle.PORTAL,
        player.getLocation().add(0, 1, 0),
        30,
        0.5, 0.5, 0.5,
        0.5
    );


        // ✅ Create link
        links.put(player.getUniqueId(), target.getUniqueId());
        links.put(target.getUniqueId(), player.getUniqueId());

        player.sendMessage("§aSoulbound with " + target.getName());
        target.sendMessage("§aSoulbound with " + player.getName());

        // 🔊 ADDED: linking sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1.5f);

        // ✅ Start beam effect
        startBeam(player, target);
    }

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {

        UUID partnerId = links.get(player.getUniqueId());
        if (partnerId == null) return;

        Player partner = Bukkit.getPlayer(partnerId);
        if (partner == null) return;

        // ❤️ ADDED: healing ability
        player.setHealth(Math.min(player.getHealth() + 4, player.getMaxHealth()));
        partner.setHealth(Math.min(partner.getHealth() + 4, partner.getMaxHealth()));

        // 🔊 ADDED: heal sound
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1f, 1.2f);
        partner.playSound(partner.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.6f);

        // ✨ ADDED: particles on players
        player.getWorld().spawnParticle(Particle.SHRIEK, player.getLocation().add(0, 1, 0), 50);
        partner.getWorld().spawnParticle(Particle.SHRIEK, partner.getLocation().add(0, 1, 0), 50);
    }

    // ----------------------------------------
    // Helper methods
    // ----------------------------------------

    private Player getNearestPlayer(Player player) {
        double range = 10;

        Player nearest = null;
        double closest = Double.MAX_VALUE;

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;

            double dist = p.getLocation().distance(player.getLocation());
            if (dist < range && dist < closest) {
                closest = dist;
                nearest = p;
            }
        }

        return nearest;
    }

    // ✅ ADDED: beam scheduler
    private void startBeam(Player p1, Player p2) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 200 || !p1.isOnline() || !p2.isOnline()) {
                    cancel();
                    return;
                }

                if (!links.containsKey(p1.getUniqueId())) {
                    cancel();
                    return;
                }

                drawBeam(p1, p2);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ✨ ADDED: beam particle renderer
    private void drawBeam(Player p1, Player p2) {
        var loc1 = p1.getLocation().add(0, 1, 0);
        var loc2 = p2.getLocation().add(0, 1, 0);

        var direction = loc2.toVector().subtract(loc1.toVector());
        double length = direction.length();

        direction.normalize().multiply(0.5);

        var current = loc1.clone();

        for (double i = 0; i < length; i += 0.5) {
            p1.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, current, 1, 0, 0, 0, 0);
            current.add(direction);
        }
    }

    public Player getLinkedPlayer(Player player) {
        UUID partnerId = links.get(player.getUniqueId());
        if (partnerId == null) return null;
        return Bukkit.getPlayer(partnerId);
    }

    if (victim.isDead() || partner.isDead() || victim.getLocation().distance(partner.getLocation()) > 30) {
        relic.unlink(victim);
    }
}
