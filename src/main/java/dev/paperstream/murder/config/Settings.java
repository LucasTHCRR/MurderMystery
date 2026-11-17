package dev.paperstream.murder.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class Settings {

    private final ConfigurationSection global;

    public Settings(ConfigurationSection global) {
        this.global = global;
    }

    private Object raw(Overridable source, String path) {
        if (source != null) {
            ConfigurationSection overrides = source.overrides();
            if (overrides != null && overrides.contains(path)) {
                return overrides.get(path);
            }
        }
        return global.get(path);
    }

    public int getInt(Overridable source, String path, int def) {
        Object value = raw(source, path);
        return value instanceof Number number ? number.intValue() : def;
    }

    public double getDouble(Overridable source, String path, double def) {
        Object value = raw(source, path);
        return value instanceof Number number ? number.doubleValue() : def;
    }

    public boolean getBoolean(Overridable source, String path, boolean def) {
        Object value = raw(source, path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return def;
    }

    public String getString(Overridable source, String path, String def) {
        Object value = raw(source, path);
        return value == null ? def : String.valueOf(value);
    }

    public List<String> getStringList(Overridable source, String path) {
        Object value = raw(source, path);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public ConfigurationSection section(String path) {
        return global.getConfigurationSection(path);
    }

    public ConfigurationSection root() {
        return global;
    }

    public boolean feature(String name) {
        return getBoolean(null, "extras." + name, true);
    }
}
