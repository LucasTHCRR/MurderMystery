package com.lucas.murder.scoreboard;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NametagManager {

    private static final String TEAM = "mm_hidden";

    private final MurderPlugin plugin;

    public NametagManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Arena arena, Player player) {
        refresh(arena);
    }

    public void refresh(Arena arena) {
        if (!plugin.settings().getBoolean(arena, "nametags.hide-above-head", true)) {
            return;
        }
        boolean hideInLobby = plugin.settings().getBoolean(arena, "nametags.hide-in-lobby", false);
        boolean running = arena.state() == GameState.RUNNING || arena.state() == GameState.ENDING;
        boolean hide = running || hideInLobby;

        for (Player viewer : arena.onlinePlayers()) {
            Scoreboard board = plugin.boards().boardOf(viewer);
            Team team = board.getTeam(TEAM);
            if (team == null) {
                team = board.registerNewTeam(TEAM);
            }
            team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                    hide ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);

            GamePlayer viewerParticipant = arena.participant(viewer.getUniqueId());
            boolean viewerIsSpectator = viewerParticipant != null
                    && (viewerParticipant.spectator() || !viewerParticipant.alive());
            boolean showToSpectators = !plugin.settings()
                    .getBoolean(arena, "nametags.hide-for-spectators", false);
            if (viewerIsSpectator && showToSpectators) {
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            }

            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
            for (Player member : arena.onlinePlayers()) {
                team.addEntry(member.getName());
            }
        }
    }

    public void clear(Player player) {
        Scoreboard board = plugin.boards().boardOf(player);
        Team team = board.getTeam(TEAM);
        if (team != null) {
            team.unregister();
        }
    }

    public void remove(Arena arena, Player player) {
        for (Player viewer : arena.onlinePlayers()) {
            Team team = plugin.boards().boardOf(viewer).getTeam(TEAM);
            if (team != null && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    public String alias(Arena arena, GamePlayer participant) {
        String pattern = plugin.settings().getString(arena, "nametags.chat-alias", "&7Player %number%");
        return Text.fill(pattern, Ph.of("number", participant.aliasNumber()));
    }

    public String chatName(Arena arena, GamePlayer participant, Player player) {
        if (arena != null && participant != null && participant.aliasNumber() > 0
                && plugin.settings().getBoolean(arena, "nametags.hide-in-chat", false)) {
            return alias(arena, participant);
        }
        return player.getName();
    }

    public String tablistName(Arena arena, GamePlayer participant, Player player) {
        if (arena != null && participant != null && participant.aliasNumber() > 0
                && plugin.settings().getBoolean(arena, "nametags.hide-in-tablist", false)) {
            String pattern = plugin.settings().getString(arena, "nametags.tablist-alias", "&7Player %number%");
            return Text.fill(pattern, Ph.of("number", participant.aliasNumber()));
        }
        return player.getName();
    }
}
