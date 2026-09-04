package com.lucas.murder.region;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionManager {

    private final Map<UUID, Location> first = new HashMap<>();
    private final Map<UUID, Location> second = new HashMap<>();

    public void setFirst(UUID player, Location location) {
        first.put(player, location.getBlock().getLocation());
    }

    public void setSecond(UUID player, Location location) {
        second.put(player, location.getBlock().getLocation());
    }

    public Location first(UUID player) {
        return first.get(player);
    }

    public Location second(UUID player) {
        return second.get(player);
    }

    public boolean complete(UUID player) {
        return first.containsKey(player) && second.containsKey(player);
    }

    public boolean mismatchedWorlds(UUID player) {
        Location a = first.get(player);
        Location b = second.get(player);
        return a != null && b != null && a.getWorld() != null && b.getWorld() != null
                && !a.getWorld().equals(b.getWorld());
    }

    public Cuboid selection(UUID player) {
        Location a = first.get(player);
        Location b = second.get(player);
        if (a == null || b == null || mismatchedWorlds(player)) {
            return null;
        }
        return Cuboid.of(a, b);
    }

    public void clear(UUID player) {
        first.remove(player);
        second.remove(player);
    }

    public void clearAll() {
        first.clear();
        second.clear();
    }
}
