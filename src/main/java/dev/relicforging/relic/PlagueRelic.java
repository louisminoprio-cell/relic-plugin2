
package dev.relicforging.relic;

import dev.relicforging.RelicForgingPlugin;
import dev.relicforging.api.Relic;
import dev.relicforging.api.RelicType;
import dev.relicforging.data.PlayerRelicData;
import dev.relicforging.integration.TeamCompatibility;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlagueRelic extends Relic {
    private final Map<UUID,Integer> infected=new HashMap<>();

    public PlagueRelic(RelicForgingPlugin plugin){super(plugin);}

    public RelicType getType(){return RelicType.PLAGUE;}
    public int getCustomModelData(){return 1009;}
    protected Material getMaterial(){return Material.FERMENTED_SPIDER_EYE;}

    public void applyPassive(Player p, PlayerRelicData d){
        p.removePotionEffect(PotionEffectType.POISON);
        p.removePotionEffect(PotionEffectType.WITHER);
        if(d.getLevel(getType())>=10){
            p.getWorld().spawnParticle(Particle.SNEEZE,p.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
        }
    }

    public void removePassive(Player p){}

    protected void doPrimary(Player p, PlayerRelicData d){
        Entity t=p.getTargetEntity(8);
        if(!(t instanceof LivingEntity le) || TeamCompatibility.isAllied(p, t)) return;
        int duration=d.getLevel(getType())>=10?180:120;
        infect(le,p,duration,d.getLevel(getType()));
    }

    private void infect(LivingEntity le, Player source, int duration, int level){
        if (TeamCompatibility.isAllied(source, le)) return;
        infected.put(le.getUniqueId(),duration);
        new BukkitRunnable(){int ticks=duration;
            public void run(){
                if(ticks<=0 || !le.isValid() || le.isDead()){infected.remove(le.getUniqueId());cancel();return;}
                double tickDamage = level >= 30 ? 2.5 : 1.5;
                le.damage(tickDamage,source);
                le.getWorld().spawnParticle(Particle.SNEEZE,le.getLocation().add(0,1,0),8,0.3,0.3,0.3,0.02);
                double spread=level>=30?5:3;
                for(Entity e:le.getNearbyEntities(spread,spread,spread)) {
                    if(!(e instanceof LivingEntity l2)) continue;
                    if(TeamCompatibility.isAllied(source, l2)) continue;
                    if(infected.containsKey(l2.getUniqueId())) continue;
                    infect(l2,source,Math.max(40,ticks/2),level);
                }
                ticks-=40;
            }
        }.runTaskTimer(plugin,0L,40L);
    }

    protected void doSecondary(Player p, PlayerRelicData d){
        for(Entity e:p.getWorld().getNearbyEntities(p.getLocation(),15,15,15)){
            if(!(e instanceof LivingEntity le)) continue;
            if(TeamCompatibility.isAllied(p, le)) continue;

            Integer rem=infected.remove(le.getUniqueId());
            if(rem!=null){
                double dmg=Math.max(3, (rem/40.0)*3);
                le.damage(dmg,p);
                le.getWorld().spawnParticle(Particle.WITCH,le.getLocation(),25,0.5,0.5,0.5,0.05);
                if(d.getLevel(getType())>=20) le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,60,0));
            }
        }
    }
}
