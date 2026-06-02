
package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BurrowRelic extends Relic {

    private static final Set<Material> MINEABLE_ORES = new HashSet<>(Arrays.asList(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    ));

    public BurrowRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() { return RelicType.BURROW; }

    @Override
    public int getCustomModelData() { return 1006; }

    @Override
    protected Material getMaterial() {
        return Material.IRON_PICKAXE;
    }

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
        if (player.getLocation().getY() < 60) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE, 40, 0, true, false, false
            ));
        }

        if (Math.random() < 0.5) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        Block b = player.getLocation().clone().add(dx, dy, dz).getBlock();
                        if (MINEABLE_ORES.contains(b.getType())) {
                            player.spawnParticle(Particle.DUST,
                                b.getLocation().add(0.5, 0.5, 0.5),
                                1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 0.8f));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        int level = data.getLevel(RelicType.BURROW);
        int cap   = (level >= 10) ? 120 : 80;

        Block target = player.getTargetBlockExact(6);
        if (target == null || !MINEABLE_ORES.contains(target.getType())) {
            player.sendMessage("§eLook at an ore block to use Vein Break.");
            data.regenEnergy(primaryCost, data.getEnergy() + primaryCost);
            return;
        }

        Set<Block> vein = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        Material oreType = target.getType();
        queue.add(target);

        while (!queue.isEmpty() && vein.size() < cap) {
            Block current = queue.poll();
            if (!vein.add(current)) continue;

            int[][] offsets = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int[] o : offsets) {
                Block neighbour = current.getRelative(o[0], o[1], o[2]);
                if (!vein.contains(neighbour) && isSameVein(neighbour.getType(), oreType)) {
                    queue.add(neighbour);
                }
            }
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        for (Block b : vein) {
            b.breakNaturally(tool);
            player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,
                b.getLocation().add(0.5, 0.5, 0.5),
                8, 0.3, 0.3, 0.3, 0.1,
                b.getBlockData());
        }

        data.addXp(vein.size() * 3);

        player.sendMessage("§e✦ Vein Break mined §6" + vein.size() + " §eblocks!");
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 0.9f);
    }

    private boolean isSameVein(Material candidate, Material source) {
        if (candidate == source) return true;
        String s = source.name();
        String c = candidate.name();
        if (s.startsWith("DEEPSLATE_")) return c.equals(s.replace("DEEPSLATE_", ""));
        return c.equals("DEEPSLATE_" + s);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        double radius = 6.0;

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius,
            e -> e instanceof LivingEntity && !e.equals(player) && !TeamCompatibility.isAllied(player, e)
        );

        for (Entity e : nearby) {
            LivingEntity le = (LivingEntity) e;
            le.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 80, 2, false, true, true
            ));
            le.setVelocity(le.getVelocity().setY(0.5));
        }

        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist  = Math.random() * radius;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,
                player.getLocation().add(x, 0.1, z),
                5, 0.1, 0.5, 0.1, 0.05,
                Material.GRAVEL.createBlockData());
        }

        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.6f);
        player.getWorld().playSound(player.getLocation(),
            Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);

        player.sendMessage("§e✦ Tremor shook " + nearby.size() + " entit" + (nearby.size() == 1 ? "y" : "ies") + "!");
    }
}
