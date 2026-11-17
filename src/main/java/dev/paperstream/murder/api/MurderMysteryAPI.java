package dev.paperstream.murder.api;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.game.Role;
import org.bukkit.entity.Player;

import java.util.List;

public final class MurderMysteryAPI {

    private static MurderPlugin plugin;

    private MurderMysteryAPI() {
    }

    public static void init(MurderPlugin instance) {
        plugin = instance;
    }

    private static MurderPlugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("MurderMystery is not enabled");
        }
        return plugin;
    }

    public static boolean isInGame(Player player) {
        return plugin().arenas().arenaOf(player) != null;
    }

    public static String getArena(Player player) {
        Arena arena = plugin().arenas().arenaOf(player);
        return arena == null ? null : arena.name();
    }

    public static Role getRole(Player player) {
        GamePlayer participant = plugin().arenas().participantOf(player);
        return participant == null ? null : participant.role();
    }

    public static boolean isAlive(Player player) {
        GamePlayer participant = plugin().arenas().participantOf(player);
        return participant != null && participant.alive() && !participant.spectator();
    }

    public static int getGold(Player player) {
        GamePlayer participant = plugin().arenas().participantOf(player);
        return participant == null ? 0 : participant.gold();
    }

    public static int getKills(Player player) {
        GamePlayer participant = plugin().arenas().participantOf(player);
        return participant == null ? 0 : participant.kills();
    }

    public static List<String> getArenas() {
        return plugin().arenas().names();
    }

    public static GameState getState(String arena) {
        Arena found = plugin().arenas().get(arena);
        return found == null ? null : found.state();
    }

    public static boolean join(Player player, String arena) {
        Arena found = plugin().arenas().get(arena);
        if (found == null || !found.canJoin(player)) {
            return false;
        }
        found.join(player);
        return true;
    }

    public static void leave(Player player) {
        Arena arena = plugin().arenas().arenaOf(player);
        if (arena != null) {
            arena.leave(player, false);
        }
    }
}
