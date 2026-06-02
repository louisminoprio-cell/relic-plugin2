
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

public class HollowRelic extends Relic {
    public HollowRelic(RelicForgingPlugin plugin){super(plugin);}
    public RelicType getType(){return RelicType.HOLLOW;}
    public int getCustomModelData(){return 1008;}
    protected Material getMaterial(){return Material.END_CRYSTAL;}

    public void applyPassive(Player p, PlayerRelicData d){
        if(p.getVelocity().getY()<0){
            p.setVelocity(p.getVelocity().setY(p.getVelocity().getY()*0.6));
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,40,0,false,false,false));
        p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation().add(0,1,0),2,0.2,0.4,0.2,0.02);
    }

    public void removePassive(Player p){
        p.removePotionEffect(PotionEffectType.SLOW_FALLING);
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    protected void doPrimary(Player p, PlayerRelicData d){
        Location target = p.getTargetBlockExact(12)!=null?p.getTargetBlockExact(12).getLocation().add(0.5,0.5,0.5):p.getLocation().add(p.getLocation().getDirection().multiply(6));
        int level=d.getLevel(getType());
        double radius=level>=10?7:5;
        new BukkitRunnable(){int ticks=0;
            public void run(){
                if(ticks++>=60){
                    if(level>=30){
                        for(Entity e: target.getWorld().getNearbyEntities(target, radius,radius,radius)){
                            if(e instanceof LivingEntity && e!=p && !TeamCompatibility.isAllied(p, e)){
                                e.setGravity(false);
                                Bukkit.getScheduler().runTaskLater(plugin,()->{
                                    if(e.isValid()) {
                                        e.setGravity(true);
                                    }
                                },20L);
                            }
                        }
                    }
                    cancel();return;
                }
                target.getWorld().spawnParticle(Particle.DRAGON_BREATH,target,20,0.6,0.6,0.6,0.05);
                for(Entity e: target.getWorld().getNearbyEntities(target,radius,radius,radius)){
                    if(e.equals(p) || TeamCompatibility.isAllied(p, e))continue;
                    Vector v=target.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.25);
                    e.setVelocity(e.getVelocity().add(v));
                }
            }
        }.runTaskTimer(plugin,0L,1L);
    }

    protected void doSecondary(Player p, PlayerRelicData d){
        int level=d.getLevel(getType());
        for(Entity e:p.getWorld().getNearbyEntities(p.getLocation(),8,8,8)){
            if(!(e instanceof LivingEntity) || e.equals(p) || TeamCompatibility.isAllied(p, e))continue;
            e.setGravity(false);
            double speed=level>=20?0.7:0.35;
            e.setVelocity(new Vector(0,speed,0));
            Bukkit.getScheduler().runTaskLater(plugin,()->{
                if(!e.isValid()) return;
                e.setGravity(true);
                if(level>=30 && p.isOnline() && !e.isDead()) { e.setFallDistance(12f); }
                e.setVelocity(new Vector(0,-3,0));
            },80L);
        }
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,1f,0.5f);
    }
}
