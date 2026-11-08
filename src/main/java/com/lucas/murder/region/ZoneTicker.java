package com.lucas.murder.region;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.util.Ph;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ZoneTicker {

    private final MurderPlugin plugin;

    private final Map<UUID, Long> lastBlock = new HashMap<>();

    private final Map<UUID, Integer> outside = new HashMap<>();

    public ZoneTicker(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void tick(Arena arena) {
        if (arena.state() != GameState.RUNNING) {
            return;
        }
        for (GamePlayer participant : arena.participants()) {
            Player player = participant.player();
            if (player == null || !participant.alive() || participant.spectator()) {
                continue;
            }
            Location location = player.getLocation();
            long key = blockKey(location);
            Long previous = lastBlock.get(participant.uuid());
            boolean moved = previous == null || previous != key;
            if (moved) {
                lastBlock.put(participant.uuid(), key);
                participant.touchMove();
            }
            handle(arena, participant, player, location, moved);
        }
    }

    private void handle(Arena arena, GamePlayer participant, Player player, Location location, boolean moved) {

        plugin.deathRules().check(arena, participant, player);
        if (!participant.alive()) {
            return;
        }
        Zone active = null;
        boolean safe = false;
        for (Zone zone : arena.zones().values()) {
            if (!zone.area().contains(location)) {
                continue;
            }
            if (zone.type() == ZoneType.SAFE) {
                safe = true;
                continue;
            }
            if (active == null || zone.type() == ZoneType.INSTANT) {
                active = zone;
            }
        }

        if (active != null && !safe) {
            switch (active.type()) {
                case INSTANT -> {
                    arena.kill(participant, null, active.messageKey());
                    return;
                }
                case DAMAGE -> {
                    if (moved) {
                        double left = player.getHealth() - active.damage();
                        if (left <= 0) {
                            arena.kill(participant, null, active.messageKey());
                            return;
                        }
                        player.setHealth(left);
                    }
                }
                case PUSHBACK -> {
                    Location safeSpot = participant.lastSafeLocation();
                    if (safeSpot != null) {
                        player.teleport(safeSpot);
                    }
                    player.sendActionBar(plugin.msg().get(player, "zone-pushback", Ph.none()));
                    plugin.sounds().play(player, "zone-warning");
                    return;
                }
                case BORDER -> {
                    pushBackToBounds(arena, participant, player);
                    return;
                }
                default -> {
                }
            }
        } else if (moved && (arena.bounds() == null || arena.bounds().contains(location))) {
            participant.lastSafeLocation(location.clone());
        }

        checkBounds(arena, participant, player, location);
    }

    private void checkBounds(Arena arena, GamePlayer participant, Player player, Location location) {
        Cuboid bounds = arena.bounds();
        if (bounds == null) {
            return;
        }
        if (bounds.contains(location)) {
            outside.remove(participant.uuid());
            participant.outsideSince(0L);
            return;
        }
        int seconds = outside.merge(participant.uuid(), 1, Integer::sum);

        int remaining = 5 - seconds / 4;
        if (remaining <= 0) {
            outside.remove(participant.uuid());
            arena.kill(participant, null, "border-death");
            return;
        }
        player.sendActionBar(plugin.msg().get(player, "border-warning", Ph.of("seconds", remaining)));
        if (location.getY() >= bounds.minY()) {
            // Unter der Map entscheidet die Void-Regel, ein Schub nach oben ließe sie schweben

            pushBackToBounds(arena, participant, player);
        }
    }

    private void pushBackToBounds(Arena arena, GamePlayer participant, Player player) {
        Location safeSpot = participant.lastSafeLocation();
        if (safeSpot == null && arena.bounds() != null) {
            safeSpot = arena.bounds().center(player.getWorld());
        }
        if (safeSpot != null) {
            org.bukkit.util.Vector push = safeSpot.toVector().subtract(player.getLocation().toVector());
            if (push.lengthSquared() > 1.0E-4D) {
                player.setVelocity(push.normalize().multiply(0.6D));
            }
        }
        plugin.sounds().play(player, "zone-warning");
    }

    public void clear(UUID player) {
        lastBlock.remove(player);
        outside.remove(player);
    }

    public void clearAll() {
        lastBlock.clear();
        outside.clear();
    }

    private static long blockKey(Location location) {
        return ((long) location.getBlockX() << 38) | ((long) location.getBlockZ() << 12) | (location.getBlockY() & 0xFFF);
    }
}
