package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.api.RelicType;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class MobilityRelic extends Relic {

    @Override
    public void applyPassive(Player player, PlayerRelicData data) {
    // TODO: add passive effect logic
    }

    private final Map<UUID, Location> recall = new HashMap<>();

    public MobilityRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public int getCustomModelData() {
        return 103;
    }

    @Override
    public void doPrimary(Player player, PlayerRelicData data) {
        Location loc = player.getLocation().add(
                player.getLocation().getDirection().multiply(5)
        );

        player.teleport(loc);
    }

    @Override
    public void doSecondary(Player player, PlayerRelicData data) {
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
    public RelicType getType() {
        return RelicType.VANGUARD;
    }
    @Override
    public void removePassive(Player player) {
    }
}
