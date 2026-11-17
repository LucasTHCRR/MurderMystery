package dev.paperstream.murder.game;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.Tags;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class CombatListener implements Listener {

    private static final String COOLDOWN_ARROW = "arrow";
    private static final String COOLDOWN_KNIFE = "knife";

    private final MurderPlugin plugin;

    public CombatListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victimPlayer)) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(victimPlayer);
        if (arena == null) {
            return;
        }
        event.setCancelled(true);
        if (arena.state() != GameState.RUNNING) {
            return;
        }
        GamePlayer victim = arena.participant(victimPlayer.getUniqueId());
        if (victim == null || !victim.alive() || victim.spectator()) {
            return;
        }

        Player attacker = attacker(event);
        if (attacker == null) {
            return;
        }
        if (attacker.equals(victimPlayer)) {
            return;
        }
        GamePlayer shooter = arena.participant(attacker.getUniqueId());
        if (shooter == null || !shooter.alive() || shooter.spectator()) {
            return;
        }
        int protection = plugin.settings().getInt(arena, "death-rules.spawn-protection", 3);
        if (protection > 0 && System.currentTimeMillis() - victim.joinedAt() < protection * 1000L) {
            return;
        }

        if (event.getDamager() instanceof Snowball snowball && Tags.isKind(snowball, Tags.KIND_KNIFE)) {
            return;
        }
        boolean projectile = event.getDamager() instanceof Projectile;
        if (projectile) {
            handleArrowHit(arena, shooter, attacker, victim, victimPlayer);
            event.getDamager().remove();
        } else {
            handleKnifeHit(arena, shooter, attacker, victim);
        }
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void handleKnifeHit(Arena arena, GamePlayer shooter, Player attacker, GamePlayer victim) {
        if (shooter.role() != Role.MURDERER) {
            return;
        }
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (!"knife".equals(Tags.read(hand, Tags.ROLE_ITEM))) {
            return;
        }
        if (victim.role() == Role.MURDERER) {
            return;
        }
        plugin.sounds().play(attacker, "kill");
        arena.kill(victim, attacker, "death-by-murderer");
    }

    private void handleArrowHit(Arena arena, GamePlayer shooter, Player attacker,
                                GamePlayer victim, Player victimPlayer) {
        if (victim.role() == Role.MURDERER) {
            if (shooter.role() == Role.MURDERER) {
                return;
            }
            shooter.hero(true);
            plugin.sounds().play(attacker, "kill");
            arena.kill(victim, attacker, "death-by-bow");
            return;
        }

        boolean punish = plugin.settings().getBoolean(arena, "bow.innocent-hit-kills-shooter", true);
        String key = shooter.role() == Role.DETECTIVE ? "death-by-detective" : "death-by-bow";
        arena.kill(victim, attacker, key);
        if (punish && shooter.role() != Role.MURDERER) {
            plugin.msg().send(attacker, "detective-killed-innocent");
            arena.kill(shooter, null, "death-generic");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (arena.state() != GameState.RUNNING || participant == null
                || !participant.alive() || participant.spectator()) {
            event.setCancelled(true);
            return;
        }
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            Tags.tag(arrow, arena.name(), Tags.KIND_ARROW);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
        if (plugin.kali().hasInfiniteBow(player.getUniqueId())) {
            return;
        }
        if (participant.role() != Role.DETECTIVE) {
            return;
        }
        int seconds = plugin.settings().getInt(arena, "bow.arrow-regen-seconds", 5);
        long round = arena.roundStart();
        plugin.cooldowns().setSeconds(player.getUniqueId(), COOLDOWN_ARROW, seconds);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> regenerateArrow(arena, player, round), seconds * 20L);
    }

    private void regenerateArrow(Arena arena, Player player, long round) {
        if (arena.state() != GameState.RUNNING || arena.roundStart() != round) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()
                || participant.role() != Role.DETECTIVE) {
            return;
        }
        player.getInventory().setItem(plugin.items().slot("arrow", 9), plugin.items().arrow());
        plugin.msg().send(player, "arrow-regen");
        plugin.sounds().play(player, "arrow-ready");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() != null && Tags.isKind(event.getHitEntity(), Tags.KIND_CORPSE)) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Snowball snowball && Tags.isKind(snowball, Tags.KIND_KNIFE)) {
            handleKnifeLanding(snowball, event);
            return;
        }
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (event.getHitBlock() != null && Tags.isTagged(arrow)) {

            plugin.getServer().getScheduler().runTaskLater(plugin, arrow::remove, 100L);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (throwKnife(event.getPlayer(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || event instanceof org.bukkit.event.player.PlayerInteractAtEntityEvent) {
            return;
        }
        if (throwKnife(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    private boolean throwKnife(Player player, ItemStack hand) {
        if (hand == null || !"knife".equals(Tags.read(hand, Tags.ROLE_ITEM))) {
            return false;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null || arena.state() != GameState.RUNNING) {
            return false;
        }
        if (!plugin.settings().feature("throwable-knife")
                || !plugin.settings().getBoolean(arena, "knife.throwable", true)) {
            return true;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || participant.role() != Role.MURDERER || !participant.alive()) {
            return true;
        }
        double cooldown = plugin.settings().getDouble(arena, "knife.throw-cooldown", 5);
        if (!plugin.cooldowns().tryUse(player.getUniqueId(), COOLDOWN_KNIFE, (long) (cooldown * 1000D))) {
            plugin.msg().send(player, "knife-cooldown",
                    Ph.of("seconds", plugin.cooldowns().remainingSeconds(player.getUniqueId(), COOLDOWN_KNIFE)));
            return true;
        }

        ItemStack knife = hand.clone();
        long round = arena.roundStart();
        plugin.sounds().play(player, "knife-throw");
        player.sendActionBar(plugin.msg().get(player, "knife-thrown", Ph.none()));
        int windup = (int) Math.round(plugin.settings().getDouble(arena, "knife.throw-windup", 0.2) * 20D);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> launchKnife(arena, player, knife, round),
                Math.max(0, windup));
        return true;
    }

    private void launchKnife(Arena arena, Player player, ItemStack knife, long round) {
        if (arena.state() != GameState.RUNNING || arena.roundStart() != round || !player.isOnline()) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.role() != Role.MURDERER) {
            return;
        }
        Vector velocity = player.getLocation().getDirection().multiply(1.6D);
        Snowball projectile = player.launchProjectile(Snowball.class, velocity);
        projectile.setItem(knife);
        projectile.setShooter(player);
        projectile.setGravity(false);
        Tags.tag(projectile, arena.name(), Tags.KIND_KNIFE);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (projectile.isValid()) {
                projectile.remove();
            }
        }, 60L);
    }

    private void handleKnifeLanding(Snowball snowball, ProjectileHitEvent event) {
        Arena arena = plugin.arenas().get(Tags.arenaOf(snowball));
        if (arena == null) {
            return;
        }
        if (event.getHitEntity() instanceof Player victimPlayer
                && snowball.getShooter() instanceof Player thrower) {
            GamePlayer victim = arena.participant(victimPlayer.getUniqueId());
            GamePlayer shooter = arena.participant(thrower.getUniqueId());
            if (victim != null && shooter != null && victim.alive() && !victim.spectator()
                    && victim.role() != Role.MURDERER && shooter.role() == Role.MURDERER) {
                plugin.sounds().play(thrower, "kill");
                arena.kill(victim, thrower, "death-by-murderer");
                snowball.remove();
                return;
            }
        }
        snowball.remove();
    }
}
