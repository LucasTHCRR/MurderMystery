package com.lucas.murder.scoreboard;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.game.Role;
import com.lucas.murder.game.WinChecker;
import com.lucas.murder.stats.PlayerStats;
import com.lucas.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class Placeholders {

    private final MurderPlugin plugin;

    public Placeholders(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, String> of(Arena arena, GamePlayer participant, Player player) {
        Map<String, String> map = new HashMap<>();
        map.put("server", Bukkit.getServer().getName());
        map.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));

        if (player != null) {
            map.put("player", player.getName());
            map.put("displayname", Text.plain(player.displayName()));
            map.put("ping", String.valueOf(player.getPing()));
            PlayerStats stats = plugin.stats().get(player.getUniqueId());
            map.put("wins", String.valueOf(stats.wins()));
            map.put("games", String.valueOf(stats.games()));
            map.put("kd", stats.formattedKd());
            map.put("coins", String.valueOf(stats.coins()));
            map.put("level", String.valueOf(stats.level()));
        }

        if (arena == null) {
            return map;
        }
        map.put("arena", arena.name());
        map.put("map", Text.plain(arena.displayName()));
        map.put("author", arena.author());
        map.put("state", plugin.msg().rawString(arena.state().messageKey()));
        map.put("current", String.valueOf(arena.playerCount()));
        map.put("max", String.valueOf(arena.maxPlayers()));
        map.put("min", String.valueOf(arena.minPlayers()));
        map.put("alive", String.valueOf(arena.aliveCount()));
        map.put("total", String.valueOf(arena.playerCount()));
        map.put("dead", String.valueOf(Math.max(0, arena.playerCount() - arena.aliveCount())));
        map.put("countdown", String.valueOf(arena.state() == GameState.ENDING
                ? arena.endingLeft() : arena.countdown()));
        map.put("time", arena.roundTime() > 0 ? Text.clock(arena.timeLeft()) : "--:--");
        map.put("time_seconds", String.valueOf(arena.timeLeft()));
        map.put("murderer", arena.lastMurdererName());
        map.put("detective", arena.lastDetectiveName());
        map.put("winner", winnerText(arena));
        map.put("votes", String.valueOf(plugin.votes().votesFor(arena)));

        int needed = plugin.settings().getInt(arena, "gold.bow-at", 10);
        map.put("gold_needed", String.valueOf(needed));

        if (participant != null) {
            map.put("gold", String.valueOf(participant.gold()));
            map.put("kills", String.valueOf(participant.kills()));
            Role role = participant.role();
            map.put("role", role == null
                    ? plugin.msg().rawString("role-not-assigned")
                    : plugin.msg().rawString(role.messageKey()));
            map.put("bow_status", participant.hasBow()
                    ? plugin.msg().rawString("bow-status-ready")
                    : Text.fill(plugin.msg().rawString("bow-status-missing"),
                    Map.of("amount", String.valueOf(Math.max(0, needed - participant.gold())))));
            map.put("arrow_status", cooldownText(participant, "arrow"));
            map.put("knife_status", cooldownText(participant, "knife"));
            map.put("favor", String.valueOf(plugin.kali().favor(participant.uuid())));
        } else {
            map.put("gold", "0");
            map.put("kills", "0");
            map.put("role", "-");
            map.put("bow_status", "-");
            map.put("arrow_status", "-");
            map.put("knife_status", "-");
            map.put("favor", "0");
        }
        return map;
    }

    private String cooldownText(GamePlayer participant, String key) {
        int seconds = plugin.cooldowns().remainingSeconds(participant.uuid(), key);
        return seconds <= 0
                ? plugin.msg().rawString("cooldown-ready")
                : Text.fill(plugin.msg().rawString("cooldown-waiting"), Map.of("seconds", String.valueOf(seconds)));
    }

    private String winnerText(Arena arena) {
        WinChecker.Result result = arena.result();
        return switch (result) {
            case INNOCENTS -> plugin.msg().rawString("role.innocent");
            case MURDERERS -> plugin.msg().rawString("role.murderer");
            case DRAW -> plugin.msg().rawString("win-nobody");
            default -> "-";
        };
    }

    public String apply(String raw, Arena arena, GamePlayer participant, Player player) {
        String filled = Text.fill(raw, of(arena, participant, player));
        if (filled.contains("%papi_") && player != null) {
            filled = plugin.hooks().applyPlaceholderApi(player, filled);
        }
        return filled;
    }
}
