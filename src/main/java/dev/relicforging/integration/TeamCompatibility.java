
package dev.relicforging.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft-hook into the HostileTeams plugin without creating a hard compile dependency.
 * If HostileTeams is not installed, the helper safely falls back to "not allied".
 */
public final class TeamCompatibility {

    private static final String HOSTILE_TEAMS_PLUGIN = "HostileTeams";

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static volatile boolean failureLogged;
    private static volatile Object teamService;
    private static volatile Method areTeammatesMethod;

    private TeamCompatibility() {
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static boolean areTeammates(Player a, Player b) {
        return areTeammates(a.getUniqueId(), b.getUniqueId());
    }

    public static boolean areTeammates(UUID a, UUID b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }

        resolve();
        if (!available || teamService == null || areTeammatesMethod == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(areTeammatesMethod.invoke(teamService, a, b));
        } catch (ReflectiveOperationException ex) {
            logFailure("areTeammates", ex);
            available = false;
            return false;
        }
    }

    /**
     * Returns true if the target is the caster themselves or an actual teammate.
     * This is the helper to use for "buff allies only" effects.
     */
    public static boolean isAllied(Player source, Entity target) {
        if (!(target instanceof Player other)) {
            return false;
        }
        return areTeammates(source, other);
    }

    public static boolean isEnemy(Player source, Entity target) {
        return target instanceof Player other && !areTeammates(source, other);
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        synchronized (TeamCompatibility.class) {
            if (resolved) {
                return;
            }

            try {
                Plugin plugin = Bukkit.getPluginManager().getPlugin(HOSTILE_TEAMS_PLUGIN);
                if (plugin != null) {
                    Method getTeamService = plugin.getClass().getMethod("getTeamService");
                    Object service = getTeamService.invoke(plugin);

                    Method areTeammates = service.getClass().getMethod("areTeammates", UUID.class, UUID.class);

                    teamService = service;
                    areTeammatesMethod = areTeammates;
                    available = true;
                }
            } catch (ReflectiveOperationException ex) {
                logFailure("resolve", ex);
                available = false;
            } catch (RuntimeException ex) {
                logFailure("resolve", ex);
                available = false;
            } finally {
                resolved = true;
            }
        }
    }

    private static void logFailure(String stage, Exception ex) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        Bukkit.getLogger().warning("[RelicForging] HostileTeams hook failed during " + stage + ": " + ex.getMessage());
    }
}
