
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

public class SoulboundRelic extends Relic {

    public SoulboundRelic() {
        super("Soulbound Core", RelicType.UTILITY);
    }

    @Override
    public void onTick(Player player) {
        if (player.getHealth() <= player.getMaxHealth() * 0.3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        }
    }

    @Override
    public AbilityResult ability1(Player player) {
        Player ally = getNearbyPlayer(player, 6);
        if (ally == null) return AbilityResult.FAIL;

        double amount = 3;

        if (player.getHealth() <= 1) return AbilityResult.FAIL;

        player.setHealth(Math.max(1, player.getHealth() - amount));
        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + amount));

        return AbilityResult.SUCCESS;
    }

    @Override
    public AbilityResult ability2(Player player) {
        Player ally = getNearbyPlayer(player, 8);
        if (ally == null) return AbilityResult.FAIL;

        linkPlayers(player, ally); // you'll implement this below
        return AbilityResult.SUCCESS;
    }
}
