package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Locations;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ArenaManager {

    private final MurderPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private Location globalLobby;
    private Location exit;

    public ArenaManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        arenas.clear();
        ConfigurationSection root = plugin.configs().arenas();
        globalLobby = Locations.fromMap(root.get("lobby"));
        exit = Locations.fromMap(root.get("exit"));

        ConfigurationSection section = root.getConfigurationSection("arenas");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(key);
            if (arenaSection == null) {
                continue;
            }
            Arena arena = new Arena(plugin, key);
            try {
                arena.load(arenaSection);
                arenas.put(key.toLowerCase(Locale.ROOT), arena);
            } catch (Exception exception) {
                plugin.getLogger().warning("Arena " + key + " could not be loaded: " + exception.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void save() {
        ConfigurationSection root = plugin.configs().arenas();
        root.set("lobby", globalLobby == null ? null : Locations.toMap(globalLobby));
        root.set("exit", exit == null ? null : Locations.toMap(exit));
        root.set("arenas", null);
        ConfigurationSection section = root.createSection("arenas");
        for (Arena arena : arenas.values()) {
            arena.save(section.createSection(arena.name()));
        }
        plugin.configs().save("arenas.yml");
    }

    public Arena get(String name) {
        return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> all() {
        return arenas.values();
    }

    public List<String> names() {
        return new ArrayList<>(arenas.keySet());
    }

    public Arena arenaOf(Player player) {
        return arenaOf(player.getUniqueId());
    }

    public Arena arenaOf(UUID uuid) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(uuid)) {
                return arena;
            }
        }
        return null;
    }

    public GamePlayer participantOf(Player player) {
        Arena arena = arenaOf(player.getUniqueId());
        return arena == null ? null : arena.participant(player.getUniqueId());
    }

    public Arena firstFree(Player player) {
        Arena best = null;
        for (Arena arena : arenas.values()) {
            if (!arena.canJoin(player)) {
                continue;
            }
            if (best == null || arena.playerCount() > best.playerCount()) {
                best = arena;
            }
        }
        return best;
    }

    public Arena create(String name) {
        Arena arena = new Arena(plugin, name);
        arenas.put(name.toLowerCase(Locale.ROOT), arena);
        save();
        return arena;
    }

    public void delete(Arena arena) {
        arena.stop(false);
        arenas.remove(arena.name().toLowerCase(Locale.ROOT));
        plugin.roles().forget(arena.name());
        save();
    }

    public Arena clone(Arena source, String newName) {
        Arena target = new Arena(plugin, newName);
        source.copyTo(target);
        arenas.put(newName.toLowerCase(Locale.ROOT), target);
        save();
        return target;
    }

    public Arena rename(Arena source, String newName) {
        Arena target = clone(source, newName);
        target.enabled(source.enabled());
        arenas.remove(source.name().toLowerCase(Locale.ROOT));
        save();
        return target;
    }

    public Location globalLobby() {
        return globalLobby;
    }

    public void globalLobby(Location location) {
        this.globalLobby = location;
        save();
    }

    public Location exit() {
        return exit;
    }

    public void exit(Location location) {
        this.exit = location;
        save();
    }

    public Location exitOrLobby(Player player) {
        if (exit != null) {
            return exit;
        }
        if (globalLobby != null) {
            return globalLobby;
        }
        return player.getWorld().getSpawnLocation();
    }

    public void shutdown() {
        for (Arena arena : arenas.values()) {
            try {
                for (Player player : arena.onlinePlayers()) {
                    arena.leave(player, true);
                }
                arena.stop(false);
                arena.stopTicker();
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not shut down arena " + arena.name() + ": " + exception.getMessage());
            }
        }
    }
}
