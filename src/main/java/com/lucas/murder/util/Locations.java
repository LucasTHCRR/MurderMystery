package com.lucas.murder.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Locations {

    private Locations() {
    }

    public static Map<String, Object> toMap(Location location) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", location.getWorld() == null ? "world" : location.getWorld().getName());
        map.put("x", round(location.getX()));
        map.put("y", round(location.getY()));
        map.put("z", round(location.getZ()));
        map.put("yaw", round(location.getYaw()));
        map.put("pitch", round(location.getPitch()));
        return map;
    }

    public static Location fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    @SuppressWarnings("unchecked")
    public static Location fromMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Object worldName = map.get("world");
            if (worldName == null) {
                return null;
            }
            World world = Bukkit.getWorld(String.valueOf(worldName));
            if (world == null) {
                return null;
            }
            return new Location(world,
                    number(map.get("x")),
                    number(map.get("y")),
                    number(map.get("z")),
                    (float) number(map.get("yaw")),
                    (float) number(map.get("pitch")));
        }
        if (raw instanceof ConfigurationSection section) {
            return fromSection(section);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String worldName(Object raw) {
        if (raw instanceof Map<?, ?> map && map.get("world") != null) {
            return String.valueOf(map.get("world"));
        }
        if (raw instanceof ConfigurationSection section) {
            return section.getString("world");
        }
        return null;
    }

    public static Location center(Location location) {
        Location copy = location.clone();
        copy.setX(copy.getBlockX() + 0.5D);
        copy.setZ(copy.getBlockZ() + 0.5D);
        return copy;
    }

    public static String pretty(Location location) {
        if (location == null) {
            return "-";
        }
        return (location.getWorld() == null ? "?" : location.getWorld().getName())
                + " " + location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ();
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0D;
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
