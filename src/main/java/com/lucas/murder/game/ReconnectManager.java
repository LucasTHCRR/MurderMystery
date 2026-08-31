package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReconnectManager {

    private static final class Pending {
        final String arena;
        final Role role;
        final int gold;
        final int kills;
        final boolean hasBow;
        final Location location;
        final ItemStack[] contents;
        final long deadline;

        Pending(String arena, GamePlayer participant, Location location, ItemStack[] contents, long deadline) {
            this.arena = arena;
            this.role = participant.role();
            this.gold = participant.gold();
            this.kills = participant.kills();
            this.hasBow = participant.hasBow();
            this.location = location;
            this.contents = contents;
            this.deadline = deadline;
        }
    }

    private final MurderPlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public ReconnectManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onQuit(Arena arena, GamePlayer participant, Player player) {
        if (arena.state() != GameState.RUNNING) {
            return false;
        }
        boolean enabled = plugin.settings().getBoolean(arena, "reconnect.enabled", true);
        boolean murdererDies = plugin.settings().getBoolean(arena, "reconnect.murderer-instant-death", true);
        if (!enabled || (participant.role() == Role.MURDERER && murdererDies)) {
            return false;
        }
        int window = plugin.settings().getInt(arena, "reconnect.window-seconds", 60);
        if (window <= 0) {
            return false;
        }
        pending.put(player.getUniqueId(), new Pending(
                arena.name(), participant,
                player.getLocation().clone(),
                player.getInventory().getContents().clone(),
                System.currentTimeMillis() + window * 1000L));
        return true;
    }

    public boolean onJoin(Player player) {
        Pending data = pending.remove(player.getUniqueId());
        if (data == null) {
            return false;
        }
        if (System.currentTimeMillis() > data.deadline) {
            return false;
        }
        Arena arena = plugin.arenas().get(data.arena);
        if (arena == null || arena.state() != GameState.RUNNING) {
            return false;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null) {
            return false;
        }
        if (plugin.settings().getBoolean(arena, "reconnect.restore-role", true)) {
            participant.role(data.role);
        }
        if (plugin.settings().getBoolean(arena, "reconnect.restore-gold", true)) {
            participant.gold(data.gold);
        }
        participant.hasBow(data.hasBow);
        participant.alive(true);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        if (plugin.settings().getBoolean(arena, "reconnect.restore-inventory", true)) {
            player.getInventory().setContents(data.contents);
        }
        if (data.location != null && data.location.getWorld() != null) {
            player.teleport(data.location);
        }
        plugin.boards().apply(player, arena);
        plugin.nametags().apply(arena, player);
        return true;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Pending> entry : new HashMap<>(pending).entrySet()) {
            if (now < entry.getValue().deadline) {
                continue;
            }
            pending.remove(entry.getKey());
            Arena arena = plugin.arenas().get(entry.getValue().arena);
            if (arena == null) {
                continue;
            }
            GamePlayer participant = arena.participant(entry.getKey());
            if (participant == null) {
                continue;
            }
            String mode = plugin.settings().getString(arena, "reconnect.on-timeout", "DEATH");
            if ("LEAVE".equalsIgnoreCase(mode)) {
                arena.participants().remove(participant);
            } else {
                participant.alive(false);
                arena.broadcast("death-quit", com.lucas.murder.util.Ph.of("player", participant.name()));
            }
            arena.checkWin();
        }
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void forget(UUID uuid) {
        pending.remove(uuid);
    }

    public void clearAll() {
        pending.clear();
    }
}
