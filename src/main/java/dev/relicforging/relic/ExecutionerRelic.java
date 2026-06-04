public class ExecutionerRelic extends Relic {

    public ExecutionerRelic() {
        super("Executioner");
    }

    @Override
    protected void doPrimary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 4);
        if (target == null) return;

        double damage = 4;

        if (target.getHealth() <= target.getMaxHealth() * 0.35) {
            damage += 3;
        }

        target.damage(damage, player);
    }

    @Override
    protected void doSecondary(Player player, PlayerRelicData data) {
        LivingEntity target = getTarget(player, 8);
        if (target == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid()) return;
            if (target.getLocation().distance(player.getLocation()) > 8) return;

            double damage = target.getHealth() > target.getMaxHealth() * 0.5 ? 8 : 12;
            target.damage(damage, player);

        }, 20); // 1 second
    }

    // Passive handled in damage listener
}
