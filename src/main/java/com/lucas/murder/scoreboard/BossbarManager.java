package com.lucas.murder.scoreboard;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GameState;
import com.lucas.murder.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BossbarManager {

    private final MurderPlugin plugin;
    private final Placeholders placeholders;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public BossbarManager(MurderPlugin plugin, Placeholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    public void update(Arena arena) {
        if (!plugin.settings().feature("bossbar-timer")
                || !plugin.configs().scoreboard().getBoolean("bossbar.enabled", true)) {
            return;
        }
        String state = switch (arena.state()) {
            case WAITING, COUNTDOWN -> "lobby";
            case RUNNING -> "ingame";
            case ENDING -> "ending";
            default -> null;
        };
        if (state == null) {
            for (Player player : arena.onlinePlayers()) {
                clear(player);
            }
            return;
        }
        ConfigurationSection section = plugin.configs().scoreboard()
                .getConfigurationSection("bossbar." + state);
        if (section == null) {
            return;
        }

        String text = section.getString("text", "");
        String colorName = section.getString("color", "RED");
        int warnAt = section.getInt("warn-at-seconds", 0);
        if (arena.state() == GameState.RUNNING && warnAt > 0 && arena.timeLeft() <= warnAt) {
            text = section.getString("warn-text", text);
            colorName = section.getString("warn-color", colorName);
        }

        BossBar.Color color = color(colorName);
        BossBar.Overlay overlay = overlay(section.getString("style", "SOLID"));
        float progress = progress(arena, section.getString("progress", "time"));

        for (Player player : arena.onlinePlayers()) {
            var participant = arena.participant(player.getUniqueId());
            String filled = placeholders.apply(text, arena, participant, player);
            BossBar bar = bars.get(player.getUniqueId());
            if (bar == null) {
                bar = BossBar.bossBar(Text.of(filled), progress, color, overlay);
                bars.put(player.getUniqueId(), bar);
                player.showBossBar(bar);
            } else {
                bar.name(Text.of(filled));
                bar.progress(progress);
                bar.color(color);
                bar.overlay(overlay);
            }
        }
    }

    private float progress(Arena arena, String mode) {
        float value = switch (mode.toLowerCase(Locale.ROOT)) {
            case "countdown" -> arena.countdownTime() <= 0 ? 1F
                    : (float) arena.countdown() / arena.countdownTime();
            case "time" -> arena.roundTime() <= 0 ? 1F
                    : (float) arena.timeLeft() / arena.roundTime();
            case "players" -> arena.maxPlayers() <= 0 ? 1F
                    : (float) arena.playerCount() / arena.maxPlayers();
            default -> 1F;
        };
        return Math.max(0F, Math.min(1F, value));
    }

    private BossBar.Color color(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay overlay(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "SEGMENTED_6" -> BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10" -> BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12" -> BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20" -> BossBar.Overlay.NOTCHED_20;
            default -> BossBar.Overlay.PROGRESS;
        };
    }

    public void clear(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void clearAll() {
        for (Map.Entry<UUID, BossBar> entry : new HashMap<>(bars).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bars.clear();
    }
}
