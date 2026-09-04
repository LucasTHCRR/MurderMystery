package com.lucas.murder.region;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Cuboid {

    private final String world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Cuboid(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static Cuboid of(Location first, Location second) {
        String world = first.getWorld() == null ? null : first.getWorld().getName();
        return new Cuboid(world,
                first.getBlockX(), first.getBlockY(), first.getBlockZ(),
                second.getBlockX(), second.getBlockY(), second.getBlockZ());
    }

    public static Cuboid fromSection(ConfigurationSection section, String fallbackWorld) {
        if (section == null) {
            return null;
        }
        ConfigurationSection first = section.getConfigurationSection("pos1");
        ConfigurationSection second = section.getConfigurationSection("pos2");
        if (first == null || second == null) {
            return null;
        }
        String world = section.getString("world", fallbackWorld);
        return new Cuboid(world,
                first.getInt("x"), first.getInt("y"), first.getInt("z"),
                second.getInt("x"), second.getInt("y"), second.getInt("z"));
    }

    public Map<String, Object> toMap(boolean includeWorld) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (includeWorld) {
            map.put("world", world);
        }
        map.put("pos1", position(minX, minY, minZ));
        map.put("pos2", position(maxX, maxY, maxZ));
        return map;
    }

    private static Map<String, Object> position(int x, int y, int z) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }

    public String world() {
        return world;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (world != null && !world.equals(location.getWorld().getName())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(Location location, int padding) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (world != null && !world.equals(location.getWorld().getName())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX - padding && x <= maxX + padding
                && y >= minY - padding && y <= maxY + padding
                && z >= minZ - padding && z <= maxZ + padding;
    }

    public long size() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }

    public Location center(World bukkitWorld) {
        return new Location(bukkitWorld,
                (minX + maxX) / 2.0D + 0.5D,
                (minY + maxY) / 2.0D,
                (minZ + maxZ) / 2.0D + 0.5D);
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public void outline(Player viewer, Particle particle, double step) {
        World bukkitWorld = viewer.getWorld();
        if (world != null && !world.equals(bukkitWorld.getName())) {
            return;
        }
        double x1 = minX;
        double y1 = minY;
        double z1 = minZ;
        double x2 = maxX + 1.0D;
        double y2 = maxY + 1.0D;
        double z2 = maxZ + 1.0D;
        for (double x = x1; x <= x2; x += step) {
            spawn(viewer, particle, x, y1, z1);
            spawn(viewer, particle, x, y1, z2);
            spawn(viewer, particle, x, y2, z1);
            spawn(viewer, particle, x, y2, z2);
        }
        for (double y = y1; y <= y2; y += step) {
            spawn(viewer, particle, x1, y, z1);
            spawn(viewer, particle, x1, y, z2);
            spawn(viewer, particle, x2, y, z1);
            spawn(viewer, particle, x2, y, z2);
        }
        for (double z = z1; z <= z2; z += step) {
            spawn(viewer, particle, x1, y1, z);
            spawn(viewer, particle, x1, y2, z);
            spawn(viewer, particle, x2, y1, z);
            spawn(viewer, particle, x2, y2, z);
        }
    }

    private void spawn(Player viewer, Particle particle, double x, double y, double z) {
        viewer.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
    }

    public List<Location> corners(World bukkitWorld) {
        List<Location> list = new ArrayList<>(8);
        list.add(new Location(bukkitWorld, minX, minY, minZ));
        list.add(new Location(bukkitWorld, minX, minY, maxZ));
        list.add(new Location(bukkitWorld, minX, maxY, minZ));
        list.add(new Location(bukkitWorld, minX, maxY, maxZ));
        list.add(new Location(bukkitWorld, maxX, minY, minZ));
        list.add(new Location(bukkitWorld, maxX, minY, maxZ));
        list.add(new Location(bukkitWorld, maxX, maxY, minZ));
        list.add(new Location(bukkitWorld, maxX, maxY, maxZ));
        return list;
    }

    @Override
    public String toString() {
        return world + " " + minX + "/" + minY + "/" + minZ + " -> " + maxX + "/" + maxY + "/" + maxZ;
    }
}
