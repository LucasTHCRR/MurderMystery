package com.lucas.murder.scoreboard;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.game.Role;
import com.lucas.murder.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BoardManager {

    private static final int MAX_LINES = 15;

    private static final String[] ENTRIES = new String[MAX_LINES];

    static {
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < MAX_LINES; i++) {
            ENTRIES[i] = colors[i % colors.length].toString() + ChatColor.RESET;
        }
    }

    private final MurderPlugin plugin;
    private final Placeholders placeholders;
    private final BossbarManager bossbars;
    private final ActionbarManager actionbars;
    private final TablistManager tablists;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Scoreboard> previous = new HashMap<>();
    private final Map<UUID, List<String>> rendered = new HashMap<>();
    private int titleFrame;
    private int titleTick;

    public BoardManager(MurderPlugin plugin) {
        this.plugin = plugin;
        this.placeholders = new Placeholders(plugin);
        this.bossbars = new BossbarManager(plugin, placeholders);
        this.actionbars = new ActionbarManager(plugin, placeholders);
        this.tablists = new TablistManager(plugin, placeholders);
    }

    public Placeholders placeholders() {
        return placeholders;
    }

    public BossbarManager bossbars() {
        return bossbars;
    }

    public Scoreboard boardOf(Player player) {
        return boards.computeIfAbsent(player.getUniqueId(), uuid -> {
            if (plugin.configs().scoreboard().getBoolean("sidebar.restore-previous", true)) {
                previous.put(uuid, player.getScoreboard());
            }
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
            return board;
        });
    }

    public void apply(Player player, Arena arena) {
        boardOf(player);
        update(player, arena);
        tablists.apply(player, arena);
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        boards.remove(uuid);
        rendered.remove(uuid);
        bossbars.clear(player);
        tablists.clear(player);
        Scoreboard old = previous.remove(uuid);
        try {
            player.setScoreboard(old != null ? old : Bukkit.getScoreboardManager().getMainScoreboard());
        } catch (IllegalStateException ignored) {

        }
        player.sendActionBar(Component.empty());
    }

    public void toggle(Player player) {
        GamePlayer participant = plugin.arenas().participantOf(player);
        boolean hidden;
        if (participant != null) {
            hidden = !participant.scoreboardHidden();
            participant.scoreboardHidden(hidden);
        } else {
            hidden = !plugin.stats().get(player.getUniqueId()).scoreboardHidden();
        }
        plugin.stats().get(player.getUniqueId()).scoreboardHidden(hidden);
        plugin.msg().send(player, hidden ? "scoreboard-toggled-off" : "scoreboard-toggled-on");
        if (hidden) {
            Scoreboard board = boards.get(player.getUniqueId());
            if (board != null && board.getObjective("mm") != null) {
                board.getObjective("mm").unregister();
                rendered.remove(player.getUniqueId());
            }
        } else {
            Arena arena = plugin.arenas().arenaOf(player);
            if (arena != null) {
                update(player, arena);
            }
        }
    }

    public void tickFast(Arena arena) {
        titleTick++;
        for (Player player : arena.onlinePlayers()) {
            actionbars.update(player, arena);
        }
    }

    public void refresh(Arena arena) {
        for (Player player : arena.onlinePlayers()) {
            update(player, arena);
        }
        bossbars.update(arena);
        tablists.update(arena);
        plugin.signs().update(arena);
    }

    public void update(Player player, Arena arena) {
        if (!plugin.settings().feature("scoreboard")
                || !plugin.configs().scoreboard().getBoolean("sidebar.enabled", true)) {
            return;
        }
        GamePlayer participant = arena == null ? null : arena.participant(player.getUniqueId());
        if (participant != null && participant.scoreboardHidden()) {
            return;
        }
        List<String> lines = linesFor(arena, participant);
        if (lines.isEmpty()) {
            return;
        }
        Scoreboard board = boardOf(player);
        Objective objective = board.getObjective("mm");
        if (objective == null) {
            objective = board.registerNewObjective("mm", Criteria.DUMMY, title());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        objective.displayName(title());

        List<String> filled = new ArrayList<>();
        for (String raw : lines) {
            filled.add(placeholders.apply(raw, arena, participant, player));
        }
        if (filled.size() > MAX_LINES) {
            plugin.getLogger().warning("Sidebar has more than " + MAX_LINES + " lines, the rest is cut off.");
            filled = filled.subList(0, MAX_LINES);
        }

        List<String> old = rendered.get(player.getUniqueId());
        if (old != null && old.size() != filled.size()) {
            clearLines(board, old.size());
            old = null;
        }
        for (int i = 0; i < filled.size(); i++) {
            String text = filled.get(i);
            if (old != null && i < old.size() && old.get(i).equals(text)) {
                continue;
            }
            String entry = ENTRIES[i];
            Team team = board.getTeam("line" + i);
            if (team == null) {
                team = board.registerNewTeam("line" + i);
                team.addEntry(entry);
            }
            team.prefix(Text.of(text));
            objective.getScore(entry).setScore(filled.size() - i);
        }
        rendered.put(player.getUniqueId(), filled);
    }

    private void clearLines(Scoreboard board, int count) {
        for (int i = 0; i < count; i++) {
            Team team = board.getTeam("line" + i);
            if (team != null) {
                team.unregister();
            }
            board.resetScores(ENTRIES[i]);
        }
    }

    private Component title() {
        ConfigurationSection section = plugin.configs().scoreboard().getConfigurationSection("sidebar.title");
        if (section == null) {
            return Text.of("&c&lMURDER");
        }
        List<String> frames = section.getStringList("frames");
        if (frames.isEmpty()) {
            return Text.of("&c&lMURDER");
        }
        if (!section.getBoolean("animated", true)) {
            return Text.of(frames.get(0));
        }
        int interval = Math.max(1, section.getInt("interval-ticks", 3));
        if (titleTick % interval == 0) {
            titleFrame = (titleFrame + 1) % frames.size();
        }
        return Text.of(frames.get(titleFrame));
    }

    private List<String> linesFor(Arena arena, GamePlayer participant) {
        ConfigurationSection sidebar = plugin.configs().scoreboard().getConfigurationSection("sidebar");
        if (sidebar == null || arena == null) {
            return List.of();
        }
        String state = switch (arena.state()) {
            case WAITING -> "lobby";
            case COUNTDOWN -> "countdown";
            case RUNNING -> "ingame";
            case ENDING -> "ending";
            default -> "lobby";
        };
        if (participant != null && (participant.spectator() || !participant.alive())
                && arena.state() == GameState.RUNNING) {
            state = "spectator";
        }
        return resolve(sidebar, state, participant, 0);
    }

    private List<String> resolve(ConfigurationSection sidebar, String state, GamePlayer participant, int depth) {
        if (depth > 5) {
            return List.of();
        }
        Object raw = sidebar.get(state);
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object entry : list) {
                out.add(String.valueOf(entry));
            }
            return out;
        }
        ConfigurationSection section = sidebar.getConfigurationSection(state);
        if (section == null) {
            return List.of();
        }
        String inherit = section.getString("inherit");
        if (inherit != null) {
            return resolve(sidebar, inherit, participant, depth + 1);
        }
        Role role = participant == null ? null : participant.role();
        if (role != null) {
            List<String> roleLines = section.getStringList(role.name().toLowerCase(Locale.ROOT));
            if (!roleLines.isEmpty()) {
                return roleLines;
            }
        }
        return section.getStringList("default");
    }

    public void reset() {
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clear(player);
            }
        }
        boards.clear();
        rendered.clear();
        previous.clear();
        bossbars.clearAll();
    }
}
