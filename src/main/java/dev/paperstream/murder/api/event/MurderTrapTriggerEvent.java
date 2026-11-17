package dev.paperstream.murder.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public final class MurderTrapTriggerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String arena;
    private final String trap;
    private final Player player;

    public MurderTrapTriggerEvent(String arena, String trap, Player player) {
        this.arena = arena;
        this.trap = trap;
        this.player = player;
    }

    public String getArena() {
        return arena;
    }

    public String getTrap() {
        return trap;
    }

    public Player getPlayer() {
        return player;
    }

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
