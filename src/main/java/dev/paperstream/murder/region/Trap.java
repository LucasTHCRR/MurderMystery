package dev.paperstream.murder.region;

import dev.paperstream.murder.game.Role;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Trap {

    public enum Mode {
        ALL, INNOCENTS, MURDERERS;

        public static Mode parse(String raw) {
            if (raw == null) {
                return null;
            }
            String value = raw.toUpperCase(Locale.ROOT);
            return switch (value) {
                case "ALL" -> ALL;
                case "INNOCENT", "INNOCENTS" -> INNOCENTS;
                case "MURDERER", "MURDERERS" -> MURDERERS;
                default -> null;
            };
        }

        public boolean allows(Role role) {
            return switch (this) {
                case ALL -> true;
                case INNOCENTS -> role != Role.MURDERER;
                case MURDERERS -> role == Role.MURDERER;
            };
        }
    }

    private final String name;
    private Mode mode;
    private final Cuboid area;
    private final int triggerX;
    private final int triggerY;
    private final int triggerZ;
    private int cost;
    private int duration;
    private int cooldown;

    public Trap(String name, Mode mode, Cuboid area, int triggerX, int triggerY, int triggerZ,
                int cost, int duration, int cooldown) {
        this.name = name;
        this.mode = mode;
        this.area = area;
        this.triggerX = triggerX;
        this.triggerY = triggerY;
        this.triggerZ = triggerZ;
        this.cost = cost;
        this.duration = duration;
        this.cooldown = cooldown;
    }

    public static Trap fromSection(String name, ConfigurationSection section, String fallbackWorld) {
        Cuboid area = Cuboid.fromSection(section, fallbackWorld);
        ConfigurationSection trigger = section.getConfigurationSection("trigger");
        if (area == null || trigger == null) {
            return null;
        }
        Mode mode = Mode.parse(section.getString("mode", "ALL"));
        return new Trap(name, mode == null ? Mode.ALL : mode, area,
                trigger.getInt("x"), trigger.getInt("y"), trigger.getInt("z"),
                section.getInt("cost", 1),
                section.getInt("duration", 3),
                section.getInt("cooldown", 30));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", mode.name());
        map.put("cost", cost);
        map.put("duration", duration);
        map.put("cooldown", cooldown);
        Map<String, Object> trigger = new LinkedHashMap<>();
        trigger.put("x", triggerX);
        trigger.put("y", triggerY);
        trigger.put("z", triggerZ);
        map.put("trigger", trigger);
        map.putAll(area.toMap(false));
        return map;
    }

    public boolean isTrigger(String world, int x, int y, int z) {
        return x == triggerX && y == triggerY && z == triggerZ
                && (area.world() == null || area.world().equals(world));
    }

    public String name() { return name; }
    public Mode mode() { return mode; }
    public void mode(Mode mode) { this.mode = mode; }
    public Cuboid area() { return area; }
    public int cost() { return cost; }
    public void cost(int cost) { this.cost = cost; }
    public int duration() { return duration; }
    public void duration(int duration) { this.duration = duration; }
    public int cooldown() { return cooldown; }
    public void cooldown(int cooldown) { this.cooldown = cooldown; }
}
