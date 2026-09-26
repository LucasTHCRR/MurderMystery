package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.game.Role;
import com.lucas.murder.util.Ph;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class ConnectionListener implements Listener {

    private final MurderPlugin plugin;

    public ConnectionListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.stats().touchName(player);

        // NPCs müssen für den Spieler neu gespawnt werden, sonst bleiben sie unsichtbar
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.reconnect().onJoin(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null) {
            return;
        }
        boolean held = plugin.reconnect().onQuit(arena, participant, player);
        if (held) {

            return;
        }
        if (arena.state() == GameState.RUNNING && participant.alive() && !participant.spectator()) {
            participant.alive(false);
            arena.broadcast("death-quit", Ph.of("player", player.getName()));

            if (participant.role() == Role.MURDERER) {
                plugin.getLogger().fine("Murderer " + player.getName() + " logged out - counted as dead.");
            }
        }
        arena.leave(player, true);
        arena.checkWin();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Arena arena = plugin.arenas().arenaOf(event.getPlayer());
        if (arena == null) {
            return;
        }
        if (arena.spectatorSpawn() != null) {
            event.setRespawnLocation(arena.spectatorSpawn());
        } else if (arena.waitingLobby() != null) {
            event.setRespawnLocation(arena.waitingLobby());
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.nametags().refresh(arena));
    }
}
