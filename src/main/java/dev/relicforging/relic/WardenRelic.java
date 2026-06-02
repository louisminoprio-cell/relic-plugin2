
package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WardenRelic extends Relic {
    public WardenRelic(RelicForgingPlugin plugin){super(plugin);}
    public RelicType getType(){return RelicType.WARDEN;}
    public int getCustomModelData(){return 1007;}
    protected Material getMaterial(){return Material.SCULK_CATALYST;}

    public void applyPassive(Player p, PlayerRelicData d){
        p.removePotionEffect(PotionEffectType.DARKNESS);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        for(Entity e:p.getNearbyEntities(6,6,6)) {
            if (TeamCompatibility.isAllied(p, e)) continue;
            if(e instanceof LivingEntity le && le.isSprinting())
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,3,0,false,false,true));
        }
    }

    public void removePassive(Player p){}

    protected void doPrimary(Player p, PlayerRelicData d){
        Entity e=p.getTargetEntity(10);
        if(!(e instanceof LivingEntity le) || TeamCompatibility.isAllied(p, e))return;
        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,100,0));
        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,2));
        if(d.getLevel(getType())>=10) le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,100,0));
        le.setMetadata("dirged", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        if(d.getLevel(getType())>=30) le.setMetadata("dirged_lvl30", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,()->{
            le.removeMetadata("dirged", plugin);
            le.removeMetadata("dirged_lvl30", plugin);
        },100L);
        le.getWorld().playSound(le.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,1f,0.7f);
    }

    protected void doSecondary(Player p, PlayerRelicData d){
        java.util.Set<java.util.UUID> hit=new java.util.HashSet<>();
        new BukkitRunnable(){double r=0;
            public void run(){
                r+=1;
                if(r>10){cancel();return;}
                for(double t=0;t<Math.PI*2;t+=Math.PI/16){
                    Location l=p.getLocation().clone().add(Math.cos(t)*r,0.2,Math.sin(t)*r);
                    p.getWorld().spawnParticle(Particle.SONIC_BOOM,l,1,0,0,0,0);
                }
                for(Entity e:p.getWorld().getNearbyEntities(p.getLocation(),r+1,3,r+1)){
                    if(TeamCompatibility.isAllied(p, e)) continue;
                    double dist=e.getLocation().distance(p.getLocation());
                    if(dist>r-1 && dist<r+1 && e instanceof LivingEntity le && e!=p && !hit.contains(e.getUniqueId())){
                        hit.add(e.getUniqueId());
                        le.damage(4,p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,120,0));
                        Vector kb=e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.1);
                        e.setVelocity(kb);
                    }
                }
            }
        }.runTaskTimer(plugin,0L,3L);
    }
}
