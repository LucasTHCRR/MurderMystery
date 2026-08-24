package com.lucas.murder.config;

import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ConfigManager {

    private final Plugin plugin;
    private final Map<String, ConfigFile> files = new LinkedHashMap<>();
    private Messages messages;
    private Settings settings;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        register("config.yml");
        register("messages.yml");
        register("arenas.yml");
        register("npcs.yml");
        register("scoreboard.yml");
        register("items.yml");
        register("sounds.yml");
        register("gui.yml");
        register("kali.yml");
        register("potions.yml");
        register("cosmetics.yml");

        files.put("stats.yml", new ConfigFile(plugin, "stats.yml", false));
        files.put("replays.yml", new ConfigFile(plugin, "replays.yml", false));

        this.messages = new Messages(plugin, files.get("messages.yml"));
        this.settings = new Settings(files.get("config.yml").raw());
        messages.reload(config().getConfigurationSection("language"));
    }

    private void register(String name) {
        files.put(name, new ConfigFile(plugin, name, true));
    }

    public ConfigFile file(String name) {
        return files.get(name.endsWith(".yml") ? name : name + ".yml");
    }

    public Set<String> fileNames() {
        return files.keySet();
    }

    public org.bukkit.configuration.file.YamlConfiguration config() {
        return files.get("config.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration arenas() {
        return files.get("arenas.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration npcs() {
        return files.get("npcs.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration scoreboard() {
        return files.get("scoreboard.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration items() {
        return files.get("items.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration sounds() {
        return files.get("sounds.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration gui() {
        return files.get("gui.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration kali() {
        return files.get("kali.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration potions() {
        return files.get("potions.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration cosmetics() {
        return files.get("cosmetics.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration stats() {
        return files.get("stats.yml").raw();
    }

    public org.bukkit.configuration.file.YamlConfiguration replays() {
        return files.get("replays.yml").raw();
    }

    public Messages messages() {
        return messages;
    }

    public Settings settings() {
        return settings;
    }

    public String reloadAll() {
        String error = null;
        for (ConfigFile file : files.values()) {
            file.reload();
            if (error == null && file.lastError() != null) {
                error = file.name() + ": " + file.lastError();
            }
        }
        settings = new Settings(config());
        messages.reload(config().getConfigurationSection("language"));
        return error;
    }

    public boolean reload(String name) {
        ConfigFile file = file(name);
        if (file == null) {
            return false;
        }
        file.reload();
        if (file.name().equals("config.yml")) {
            settings = new Settings(config());
            messages.reload(config().getConfigurationSection("language"));
        } else if (file.name().equals("messages.yml")) {
            messages.reload(config().getConfigurationSection("language"));
        }
        return true;
    }

    public void save(String name) {
        ConfigFile file = file(name);
        if (file != null) {
            file.save();
        }
    }

    public void saveAll() {
        save("arenas.yml");
        save("npcs.yml");
        save("stats.yml");
        save("replays.yml");
    }
}
