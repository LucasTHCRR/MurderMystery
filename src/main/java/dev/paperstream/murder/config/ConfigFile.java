package dev.paperstream.murder.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class ConfigFile {

    private final Plugin plugin;
    private final String name;
    private final File file;
    private final boolean mergeDefaults;
    private YamlConfiguration config;
    private String lastError;

    public ConfigFile(Plugin plugin, String name, boolean mergeDefaults) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        this.mergeDefaults = mergeDefaults;
        reload();
    }

    public String name() {
        return name;
    }

    public YamlConfiguration raw() {
        return config;
    }

    public String lastError() {
        return lastError;
    }

    public void reload() {
        lastError = null;
        if (!file.exists()) {

            if (plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            } else {
                createEmpty();
            }
        }
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(file);
        } catch (Exception exception) {
            lastError = exception.getMessage();
            plugin.getLogger().log(Level.SEVERE, "Could not read " + name + " - using the defaults from the jar", exception);
        }
        if (mergeDefaults) {
            YamlConfiguration defaults = jarDefaults();
            if (defaults != null) {
                loaded.setDefaults(defaults);
                loaded.options().copyDefaults(true);
            }
        }
        this.config = loaded;
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create the folder for " + name);
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + name, exception);
        }
    }

    private void createEmpty() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            if (!file.createNewFile()) {
                plugin.getLogger().warning("Could not create " + name);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not create " + name, exception);
        }
    }

    private YamlConfiguration jarDefaults() {
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }
}
