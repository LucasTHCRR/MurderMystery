package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class DeathRuleListener implements Listener {

    private final MurderPlugin plugin;

    public DeathRuleListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null || arena.state() != GameState.RUNNING) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return;
        }
        if (inSafeZone(arena, player.getLocation())) {
            event.setCancelled(true);
            return;
        }

        String key = switch (event.getCause()) {
            case LAVA -> rule(arena, "lava") ? "death-lava" : null;
            case FIRE, FIRE_TICK, HOT_FLOOR -> rule(arena, "fire") ? "death-fire" : null;
            case DROWNING -> rule(arena, "water") ? "death-water" : null;
            case SUFFOCATION -> rule(arena, "suffocation") ? "death-suffocation" : null;
            case CONTACT -> rule(arena, "cactus") ? "death-cactus" : null;
            case VOID -> "death-void";
            case FALL -> rule(arena, "fall-damage") ? "death-fall" : null;
            default -> null;
        };
        event.setCancelled(true);
        if (key != null) {
            arena.kill(participant, null, key);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        if (plugin.settings().getBoolean(arena, "extras.disable-hunger", true)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20F);
        }
    }

    public void check(Arena arena, GamePlayer participant, Player player) {
        Location location = player.getLocation();
        if (inSafeZone(arena, location)) {
            return;
        }
        double voidY = plugin.settings().getDouble(arena, "death-rules.void-y", 0);
        if (location.getY() < voidY) {
            arena.kill(participant, null, "death-void");
            return;
        }
        double maxY = plugin.settings().getDouble(arena, "death-rules.max-y", 256);
        if (maxY > 0 && location.getY() > maxY) {
            arena.kill(participant, null, "death-zone");
            return;
        }
        if (rule(arena, "water") && isWater(location)) {
            arena.kill(participant, null, "death-water");
            return;
        }
        if (rule(arena, "lava") && isLava(location)) {
            arena.kill(participant, null, "death-lava");
        }
    }

    private boolean rule(Arena arena, String name) {
        return plugin.settings().getBoolean(arena, "death-rules." + name, true);
    }

    private boolean isWater(Location location) {
        Material feet = location.getBlock().getType();
        Material legs = location.clone().add(0, 1, 0).getBlock().getType();
        return feet == Material.WATER || legs == Material.WATER
                || location.getBlock().isLiquid() && feet != Material.LAVA;
    }

    private boolean isLava(Location location) {
        return location.getBlock().getType() == Material.LAVA
                || location.clone().add(0, 1, 0).getBlock().getType() == Material.LAVA;
    }

    private boolean inSafeZone(Arena arena, Location location) {
        for (var zone : arena.zones().values()) {
            if (zone.type() == com.lucas.murder.region.ZoneType.SAFE && zone.area().contains(location)) {
                return true;
            }
        }
        return false;
    }
}
