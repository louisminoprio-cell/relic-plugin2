package dev.relicforging.relic;


import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WardenRelic extends Relic {

    public WardenRelic(RelicForgingPlugin plugin) {
        super(plugin);
    }

    @Override
    public RelicType getType() {
        return RelicType.WARDEN;
    }

    @Override
    public int getCustomModelData() {
        return 1007;
    }

    @Override
    protected Material getMaterial() {
        return Material.SCULK_CATALYST;
    }

    @Override
    public void applyPassive(Player p, PlayerRelicData d) {
        p.removePotionEffect(PotionEffectType.DARKNESS);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.SLOWNESS);

        for (Entity e : p.getNearbyEntities(6, 6, 6)) {
            if (TeamCompatibility.isAllied(p, e)) continue;

            if (e instanceof LivingEntity le) {
                if (le instanceof Player && ((Player) le).isSprinting()) {
                    le.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            60,
                            0,
                            false,
                            false
                    ));
                }
            }
        }
    }
}
