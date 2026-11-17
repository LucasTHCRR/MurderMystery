package dev.paperstream.murder.stats;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.game.Role;
import dev.paperstream.murder.game.WinChecker;
import dev.paperstream.murder.util.Registries;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StatsManager {

    private final MurderPlugin plugin;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    private final Leaderboard leaderboard;

    public StatsManager(MurderPlugin plugin) {
        this.plugin = plugin;
        this.leaderboard = new Leaderboard(this);
    }

    public Leaderboard leaderboard() {
        return leaderboard;
    }

    public void load() {
        cache.clear();
        ConfigurationSection root = plugin.configs().stats().getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                cache.put(uuid, PlayerStats.load(uuid, root.getConfigurationSection(key)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("stats.yml contains an invalid uuid: " + key);
            }
        }
    }

    public void save() {
        ConfigurationSection root = plugin.configs().stats();
        for (PlayerStats stats : cache.values()) {
            ConfigurationSection section = root.getConfigurationSection("players." + stats.uuid());
            if (section == null) {
                section = root.createSection("players." + stats.uuid());
            }
            stats.save(section);
        }
        plugin.configs().save("stats.yml");
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public PlayerStats get(UUID uuid) {
        return cache.computeIfAbsent(uuid, key -> {
            var offline = plugin.getServer().getOfflinePlayer(key);
            return new PlayerStats(key, offline.getName() == null ? "?" : offline.getName());
        });
    }

    public Collection<PlayerStats> all() {
        return cache.values();
    }

    public void touchName(Player player) {
        get(player.getUniqueId()).name(player.getName());
    }

    public void addKill(UUID uuid) {
        get(uuid).addKill();
    }

    public void addGold(UUID uuid, int amount) {
        get(uuid).addGold(amount);
    }

    public void addBow(UUID uuid) {
        get(uuid).addBow();
    }

    public void onRoundEnd(Arena arena, WinChecker.Result result) {
        if (!plugin.settings().feature("stats")) {
            return;
        }
        long seconds = (System.currentTimeMillis() - arena.roundStart()) / 1000L;
        for (GamePlayer participant : arena.participants()) {
            if (participant.spectator() || participant.role() == null) {
                continue;
            }
            PlayerStats stats = get(participant.uuid());
            stats.addGame();
            stats.addPlaytime(seconds);
            boolean won = switch (result) {
                case INNOCENTS -> participant.role() != Role.MURDERER;
                case MURDERERS -> participant.role() == Role.MURDERER;
                default -> false;
            };
            if (won) {
                stats.addWin();
                stats.addRoleWin(participant.role().name());
                if (participant.hero()) {
                    stats.addHero();
                }
            } else {
                stats.addLoss();
            }
        }
        saveAsync();
    }

    public Material equippedMaterial(UUID uuid, String category) {
        if (!plugin.settings().feature("cosmetics")) {
            return null;
        }
        String id = get(uuid).equipped(category);
        if (id == null) {
            return null;
        }
        ConfigurationSection item = plugin.configs().cosmetics()
                .getConfigurationSection("categories." + category + ".items." + id);
        if (item == null) {
            return null;
        }
        return Registries.material(item.getString("material"));
    }

    public void playDeathEffect(UUID uuid, Location location) {
        if (!plugin.settings().feature("cosmetics") || location.getWorld() == null) {
            return;
        }
        String id = get(uuid).equipped("death");
        if (id == null) {
            return;
        }
        ConfigurationSection item = plugin.configs().cosmetics()
                .getConfigurationSection("categories.death.items." + id);
        if (item == null) {
            return;
        }
        String effect = item.getString("effect", "NONE");
        switch (effect.toUpperCase(java.util.Locale.ROOT)) {
            case "LIGHTNING" -> location.getWorld().strikeLightningEffect(location);
            case "EXPLOSION" -> location.getWorld().spawnParticle(Particle.EXPLOSION, location, 3);
            case "SOUL" -> location.getWorld().spawnParticle(Particle.SOUL, location.clone().add(0, 1, 0),
                    30, 0.3, 0.6, 0.3, 0.02);
            default -> {
            }
        }
    }
}
