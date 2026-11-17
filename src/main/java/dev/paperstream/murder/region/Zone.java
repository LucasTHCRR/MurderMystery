package dev.paperstream.murder.region;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Zone {

    private final String name;
    private ZoneType type;
    private Cuboid area;
    private String messageKey;
    private double damage;

    public Zone(String name, ZoneType type, Cuboid area, String messageKey, double damage) {
        this.name = name;
        this.type = type;
        this.area = area;
        this.messageKey = messageKey;
        this.damage = damage;
    }

    public static Zone fromSection(String name, ConfigurationSection section, String fallbackWorld) {
        Cuboid area = Cuboid.fromSection(section, fallbackWorld);
        if (area == null) {
            return null;
        }
        ZoneType type = ZoneType.parse(section.getString("type", "INSTANT"));
        if (type == null) {
            type = ZoneType.INSTANT;
        }
        return new Zone(name, type, area,
                section.getString("message", "death-zone"),
                section.getDouble("damage", 2.0D));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        if (messageKey != null) {
            map.put("message", messageKey);
        }
        if (type == ZoneType.DAMAGE) {
            map.put("damage", damage);
        }
        map.putAll(area.toMap(false));
        return map;
    }

    public String name() {
        return name;
    }

    public ZoneType type() {
        return type;
    }

    public void type(ZoneType type) {
        this.type = type;
    }

    public Cuboid area() {
        return area;
    }

    public void area(Cuboid area) {
        this.area = area;
    }

    public String messageKey() {
        return messageKey == null || messageKey.isEmpty() ? "death-zone" : messageKey;
    }

    public void messageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public double damage() {
        return damage;
    }
}
