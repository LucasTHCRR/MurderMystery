package dev.paperstream.murder.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import dev.paperstream.murder.game.WinChecker;

public final class MurderGameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String arena;
    private final WinChecker.Result winner;

    public MurderGameEndEvent(String arena, WinChecker.Result winner) {
        this.arena = arena;
        this.winner = winner;
    }

    public String getArena() {
        return arena;
    }

    public WinChecker.Result getWinner() {
        return winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
