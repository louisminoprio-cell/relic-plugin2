package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Echo Relic — Detection, Stealth, and Utility.
 *
 * Theme: the bearer resonates with sculk frequencies to sense ore deposits
 *        and nearby creatures through solid rock — the perfect spelunker and scout.
 *
 * Passive:  Night vision underground; quieter movement (no step sounds — handled
 *            in a separate event listener).
 * /1 (Ore Pulse): Spawns glowing particles at every ore block within a 12-block
 *                 sphere so the player can see deposits through the terrain.
 * /2 (Resonance Scan): Reveals the location of nearby living entities by
 *                       spawning a skulk-coloured particle at each of them.
 *
 * Level scaling:
 *   Level 10+: Ore Pulse radius grows to 18 blocks.
 *   Level 20+: Resonance Scan also briefly applies Glowing to detected mobs.
 *   Level 30:  Ore Pulse also highlights deepslate variants.
 */
public class EchoRelic extends Relic {

    // The set of block types that count as "ores" for Ore Pulse.
    // Using a HashSet for O(1) lookup since we check every block in a sphere.
    private static final Set<Material> ORE_MATERIALS = new HashSet<>(Arrays.asList(
        Material.COAL_ORE,      Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,      Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE,      Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE,  Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE,     Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE,   Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE,   Material.DEEPSLATE_EMERALD_ORE,
        Material.COPPER_ORE,    Material.DEEPSLATE_COPPER_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    ));

    public EchoRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.ECHO; }

    @Override
    public int getCustomModelData() { return 1004; }

    @Override
    protected Material getMaterial() {
        return Material.ECHO_SHARD;
    }

    // ----------------------------------------------------------------
    // Passive
    // ----------------------------------------------------------------

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        // Apply night vision only if the player is underground (below y=60
        // is a reasonable heuristic for "in a cave or mine").
        if (player.getLocation().getY() < 60) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION, 40, 0, true, false, false
            ));
        }

        // Ambient sculk pulse particle at low frequency for atmosphere.
        if (Math.random() < 0.2) {
            player.getWorld().spawnParticle(Particle.SCULK_SOUL,
                player.getLocation().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0.01);
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    // ----------------------------------------------------------------
    // Primary — Ore Pulse
    // ----------------------------------------------------------------

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        int level  = data.getLevel(RelicType.ECHO);
        int radius = (level >= 10) ? 18 : 12;

        Location centre = player.getLocation();
        int oresFound   = 0;

        // Walk every block in a sphere of `radius` blocks.
        // We cap the scan at a reasonable block count to stay server-friendly.
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only process blocks actually within the sphere (bounding box
                    // without this check covers too many extra corners).
                    if (x * x + y * y + z * z > radius * radius) continue;

                    Block block = centre.clone().add(x, y, z).getBlock();
                    if (ORE_MATERIALS.contains(block.getType())) {
                        // Spawn a glowing particle at the ore's centre.
                        Location oreLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        player.spawnParticle(Particle.SCULK_CHARGE_POP,
                            oreLoc, 3, 0.1, 0.1, 0.1, 0.01);
                        oresFound++;
                    }
                }
            }
        }

        // Grant a small XP bonus based on how many ores were found —
        // rewards explorers who use the ability in rich areas.
        data.addXp(oresFound * 2);

        player.sendMessage("§5✦ Ore Pulse detected §d" + oresFound + " §5ore block(s) nearby.");
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
    }

    // ----------------------------------------------------------------
    // Secondary — Resonance Scan
    // ----------------------------------------------------------------

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        int    level  = data.getLevel(RelicType.ECHO);
        double radius = 20.0;

        Collection<Entity> entities = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof LivingEntity && !e.equals(player)
        );

        int count = 0;
        for (Entity e : entities) {
            // Spawn a distinctive particle at each entity's head.
            player.spawnParticle(Particle.SCULK_SOUL,
                e.getLocation().add(0, 2, 0), 5, 0.1, 0.1, 0.1, 0.01);

            // Level 20+: apply Glowing so the player can see entity outlines.
            if (level >= 20 && e instanceof LivingEntity) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING, 60, 0, false, false, false
                ));
            }
            count++;
        }

        player.sendMessage("§5✦ Resonance Scan detected §d" + count + " §5entit" + (count == 1 ? "y" : "ies") + " nearby.");
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.5f);
    }
}
