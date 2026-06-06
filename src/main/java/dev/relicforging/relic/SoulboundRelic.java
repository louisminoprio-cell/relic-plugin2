package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulboundRelic extends Relic {

    // ✅ ONLY ONE MAP — UUID SAFE
    private final Map<UUID, UUID> links = new HashMap<>();

    // ✅ Prevent infinite damage loops
    private final Map<UUID, Long> recentlyDamaged = new HashMap<>();

    public SoulboundRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() {
        return RelicType.SOULBOUND;
    }

    @Override
    public int getCustomModelData() {
        return 1001;
    }

            @Override
    public void applyPassive(Player player, PlayerRelicData data) {

    UUID id = player.getUniqueId();

    double maxHealth = player.getMaxHealth();
    double currentHealth = player.getHealth();

    // ❤️ Trigger when under 20% HP
    if (currentHealth > maxHealth * 0.2) return;

    // ❗ Prevent re-trigger if shield already active
    if (player.getAbsorptionAmount() > 0) return;

    long now = System.currentTimeMillis();

    // ⏳ Cooldown check (5 minutes)
    if (passiveCooldown.containsKey(id)) {
        long lastUsed = passiveCooldown.get(id);
        if ((now - lastUsed) < 300000) {
            return;
        }
    }

    // 🛡️ Give 4 hearts (8 HP) absorption
    player.setAbsorptionAmount(8.0);

    // ✨ Particles
    player.getWorld().spawnParticle(
            org.bukkit.Particle.TOTEM,
            player.getLocation().add(0, 1, 0),
            30,
            0.5, 0.5, 0.5,
            0.2
    );

    // 🔊 Sound
    player.playSound(
            player.getLocation(),
            org.bukkit.Sound.ITEM_TOTEM_USE,
            1f,
            1f
    );

    // ⏳ Save cooldown
    passiveCooldown.put(id, now);
    }
    
    @Override
    public void removePassive(Player player) {
        unlink(player);
    }

    // ========================================
    // 🔗 LINK LOGIC
    // ========================================

    public void linkPlayers(Player a, Player b) {
        links.put(a.getUniqueId(), b.getUniqueId());
        links.put(b.getUniqueId(), a.getUniqueId());
    }

    public void unlink(Player p) {
        UUID otherId = links.remove(p.getUniqueId());
        if (otherId != null) {
            links.remove(otherId);
        }
    }

    public Player getLinkedPlayer(Player player) {
        UUID partnerId = links.get(player.getUniqueId());
        if (partnerId == null) return null;
        return Bukkit.getPlayer(partnerId);
    }

    public boolean isLinked(Player player) {
        return links.containsKey(player.getUniqueId());
    }

    // ========================================
    // ⚔️ DAMAGE SHARING (SAFE)
    // ========================================

    public void handleDamage(Player victim, double damage) {
        Player partner = getLinkedPlayer(victim);
        if (partner == null || !partner.isOnline()) return;

        long now = System.currentTimeMillis();

        // prevent infinite loop
        if (recentlyDamaged.containsKey(victim.getUniqueId())
                && now - recentlyDamaged.get(victim.getUniqueId()) < 100) {
            return;
        }

        recentlyDamaged.put(victim.getUniqueId(), now);
        recentlyDamaged.put(partner.getUniqueId(), now);

        double shared = damage * 0.3;

        // reduce original damage
        victim.damage(damage * 0.7);

        // apply shared damage
        partner.damage(shared);

        // ✨ particles
        victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation(), 20);
        partner.getWorld().spawnParticle(Particle.SOUL, partner.getLocation(), 20);
    }

    // ========================================
    // 🔮 ABILITIES
    // ========================================

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {

        Player target = getNearestPlayer(player);

        if (target == null) {
            player.sendMessage("§cNo nearby player to link!");
            return;
        }

        // create link
        linkPlayers(player, target);

        player.sendMessage("§aSoulbound with " + target.getName());
        target.sendMessage("§aSoulbound with " + player.getName());

        // 🔊 sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1.5f);

        // ✨ particles
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 30);

        // beam
        startBeam(player, target);
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {

        Player partner = getLinkedPlayer(player);
        if (partner == null) return;

        // ❤️ heal both
        player.setHealth(Math.min(player.getHealth() + 4, player.getMaxHealth()));
        partner.setHealth(Math.min(partner.getHealth() + 4, partner.getMaxHealth()));

        // 🔊 sounds
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1f, 1.2f);
        partner.playSound(partner.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.6f);

        // ✨ particles
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 20);
        partner.getWorld().spawnParticle(Particle.HEART, partner.getLocation(), 20);
    }

    // ========================================
    // 🔍 HELPERS
    // ========================================

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

    private void startBeam(Player p1, Player p2) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 200 || !p1.isOnline() || !p2.isOnline()) {
                    cancel();
                    return;
                }

                if (!isLinked(p1)) {
                    cancel();
                    return;
                }

                drawBeam(p1, p2);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void drawBeam(Player p1, Player p2) {
        var loc1 = p1.getLocation().add(0, 1, 0);
        var loc2 = p2.getLocation().add(0, 1, 0);

        var direction = loc2.toVector().subtract(loc1.toVector());
        double length = direction.length();

        direction.normalize().multiply(0.5);

        var current = loc1.clone();

        for (double i = 0; i < length; i += 0.5) {
            p1.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, current, 1);
            current.add(direction);
        }
    }
}
