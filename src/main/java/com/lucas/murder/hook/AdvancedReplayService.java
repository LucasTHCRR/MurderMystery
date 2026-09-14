package com.lucas.murder.hook;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdvancedReplayService implements ReplayService {

    private final MurderPlugin plugin;
    private Object api;

    public AdvancedReplayService(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        if (!plugin.settings().feature("replays")
                || !plugin.settings().getBoolean(null, "replay.enabled", true)) {
            return false;
        }
        if (Bukkit.getPluginManager().getPlugin("AdvancedReplay") == null) {
            return false;
        }
        try {
            if (api == null) {
                Class<?> apiClass = Class.forName("me.jumper251.replay.api.ReplayAPI");
                api = apiClass.getMethod("getInstance").invoke(null);
            }
            return api != null;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public String startRecording(Arena arena) {
        if (!isAvailable()) {
            return null;
        }
        String id = plugin.settings().getString(arena, "replay.name-pattern", "mm_%arena%_%date%_%id%")
                .replace("%arena%", arena.name())
                .replace("%date%", LocalDate.now().toString())
                .replace("%id%", String.valueOf(System.currentTimeMillis() % 100000));
        List<Player> players = arena.onlinePlayers();
        try {
            api.getClass().getMethod("recordReplay", String.class, Player.class, List.class)
                    .invoke(api, id, players.isEmpty() ? null : players.get(0), players);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("AdvancedReplay could not start the recording: " + throwable.getMessage());
            return null;
        }
        store(id, arena);
        return id;
    }

    private void store(String id, Arena arena) {
        ConfigurationSection root = plugin.configs().replays();
        ConfigurationSection section = root.createSection("replays." + id);
        section.set("arena", arena.name());
        section.set("date", System.currentTimeMillis());
        section.set("duration", 0);
        List<String> uuids = new ArrayList<>();
        for (GamePlayer participant : arena.participants()) {
            uuids.add(participant.uuid().toString());
        }
        section.set("players", uuids);
        plugin.configs().save("replays.yml");
    }

    @Override
    public void stopRecording(String id, boolean save) {
        if (id == null || !isAvailable()) {
            return;
        }
        try {
            api.getClass().getMethod("stopReplay", String.class, boolean.class).invoke(api, id, save);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("AdvancedReplay could not stop the recording: " + throwable.getMessage());
        }
        ConfigurationSection section = plugin.configs().replays().getConfigurationSection("replays." + id);
        if (section != null) {
            if (!save) {
                plugin.configs().replays().set("replays." + id, null);
            } else {
                long started = section.getLong("date", System.currentTimeMillis());
                section.set("duration", (int) ((System.currentTimeMillis() - started) / 1000L));
            }
            plugin.configs().save("replays.yml");
        }
    }

    @Override
    public boolean play(Player viewer, String id) {
        if (!isAvailable()) {
            return false;
        }
        try {
            api.getClass().getMethod("playReplay", String.class, Player.class).invoke(api, id, viewer);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("AdvancedReplay could not start the playback: " + throwable.getMessage());
            return false;
        }
    }

    @Override
    public void stop(Player viewer) {
        if (!isAvailable()) {
            return;
        }
        try {
            api.getClass().getMethod("stopPlayback", Player.class).invoke(api, viewer);
        } catch (Throwable ignored) {

        }
    }

    @Override
    public List<ReplayInfo> listFor(UUID player, int limit) {
        List<ReplayInfo> out = new ArrayList<>();
        ConfigurationSection root = plugin.configs().replays().getConfigurationSection("replays");
        if (root == null) {
            return out;
        }
        for (String id : root.getKeys(false)) {
            ReplayInfo info = read(id, root.getConfigurationSection(id));
            if (info == null) {
                continue;
            }
            if (player == null || info.players().contains(player)) {
                out.add(info);
            }
        }
        out.sort((a, b) -> Long.compare(b.date(), a.date()));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    @Override
    public ReplayInfo info(String id) {
        ConfigurationSection section = plugin.configs().replays()
                .getConfigurationSection("replays." + id);
        return read(id, section);
    }

    private ReplayInfo read(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        List<UUID> players = new ArrayList<>();
        for (String raw : section.getStringList("players")) {
            try {
                players.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {

            }
        }
        return new ReplayInfo(id, section.getString("arena", "?"),
                section.getLong("date"), section.getInt("duration"), players);
    }

    @Override
    public boolean delete(String id) {
        if (plugin.configs().replays().get("replays." + id) == null) {
            return false;
        }
        try {
            if (isAvailable()) {
                api.getClass().getMethod("deleteReplay", String.class).invoke(api, id);
            }
        } catch (Throwable ignored) {

        }
        plugin.configs().replays().set("replays." + id, null);
        plugin.configs().save("replays.yml");
        return true;
    }

    @Override
    public int cleanup(int keepDays, int keepMax) {
        List<ReplayInfo> all = listFor(null, Integer.MAX_VALUE);
        long cutoff = System.currentTimeMillis() - keepDays * 24L * 3600L * 1000L;
        int removed = 0;
        for (int i = 0; i < all.size(); i++) {
            ReplayInfo info = all.get(i);
            boolean tooOld = keepDays > 0 && info.date() < cutoff;
            boolean tooMany = keepMax > 0 && i >= keepMax;
            if (tooOld || tooMany) {
                if (delete(info.id())) {
                    removed++;
                }
            }
        }
        return removed;
    }
}
