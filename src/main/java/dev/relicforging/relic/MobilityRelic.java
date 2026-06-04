package dev.relicforging.relic;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MobilityRelic extends Relic {

    private final Map<UUID, Location> recall = new HashMap<>();

    public MobilityRelic() {
        super("Mobility Core");
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        Location loc = player.getLocation().add(
                player.getLocation().getDirection().multiply(5)
        );

        player.teleport(loc);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        UUID id = player.getUniqueId();

        if (!recall.containsKey(id)) {
            recall.put(id, player.getLocation());

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (recall.containsKey(id)) {
                    player.teleport(recall.remove(id));
                }
            }, 160);

        } else {
            player.teleport(recall.remove(id));
        }
    }

    @Override
public int getCustomModelData() {
    return 103; // change per relic
}
}
