package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.api.RelicType;

import org.bukkit.entity.Player;


private final java.util.Map<java.util.UUID, java.util.UUID> soulLinks = new java.util.HashMap<>();

public void linkPlayers(Player p1, Player p2) {
    soulLinks.put(p1.getUniqueId(), p2.getUniqueId());
    soulLinks.put(p2.getUniqueId(), p1.getUniqueId());
}

public Player getLinkedPlayer(Player player) {
    java.util.UUID linked = soulLinks.get(player.getUniqueId());
    if (linked == null) return null;

    return org.bukkit.Bukkit.getPlayer(linked);


public class SoulboundRelic extends Relic {

    public SoulboundRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
    // TODO: add passive effect logic
    }

    @Override
    public int getCustomModelData() {
        return 102;
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {
        Player ally = getNearbyPlayer(player, 6);
        if (ally == null) return;

        double amount = 3;

        player.getWorld().spawnParticle(
        org.bukkit.Particle.PORTAL,
        player.getLocation().add(0, 1, 0),
        25,
        0.5, 0.5, 0.5,
        0.5
    );

                player.getWorld().playSound(
        player.getLocation(),
        org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME,
        1f,
        0.9f
    );


        if (player.getHealth() <= 1) return;

        player.setHealth(Math.max(1, player.getHealth() - amount));
        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + amount));


        PPlayer target = getLinkedPlayer(player);
        if (target == null) return;

        if (target == null) return;

        // Run a short beam animation
        new org.bukkit.scheduler.BukkitRunnable() {
            double t = 0;

    @Override
    public void run() {
        if (t > 1.0 || !player.isOnline() || !target.isOnline()) {
            cancel();
            return;
        }

        org.bukkit.Location start = player.getLocation().add(0, 1, 0);
        org.bukkit.Location end = target.getLocation().add(0, 1, 0);

        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector());

        for (double i = 0; i < 1; i += 0.1) {
            org.bukkit.Location point = start.clone().add(direction.clone().multiply(i));

            player.getWorld().spawnParticle(
                org.bukkit.Particle.END_ROD,
                point,
                1,
                0, 0, 0,
                0
            );
            }

            t += 0.1;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {
        Player ally = getNearbyPlayer(player, 8);
        if (ally == null) return;

            player.getWorld().spawnParticle(
        org.bukkit.Particle.SHRIEK,
        player.getLocation().add(0, 1, 0),
        50,
        0.5, 0.5, 0.5,
        0.5
    );

                player.getWorld().playSound(
        player.getLocation(),
        org.bukkit.Sound.BLOCK_PORTAL_TRAVEL,
        0.5f,
        0.6f
    );


        player.sendMessage("§aSoul Link activated with " + ally.getName());
    }

    private Player getNearbyPlayer(Player player, double range) {
        for (Player p : player.getWorld().getPlayers()) {
            if (p != player && p.getLocation().distance(player.getLocation()) <= range) {
                return p;
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
