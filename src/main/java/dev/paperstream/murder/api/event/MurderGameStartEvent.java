package dev.paperstream.murder.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import org.bukkit.entity.Player;

public final class MurderGameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String arena;
    private final List<Player> players;

    public MurderGameStartEvent(String arena, List<Player> players) {
        this.arena = arena;
        this.players = players;
    }

    public String getArena() {
        return arena;
    }

    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
