package dev.paperstream.murder.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.bukkit.entity.Player;
import dev.paperstream.murder.game.Role;
import org.bukkit.event.Cancellable;

public final class MurderPlayerKillEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String arena;
    private final Player victim;
    private final Role victimRole;
    private final Player killer;
    private final String cause;

    public MurderPlayerKillEvent(String arena, Player victim, Role victimRole, Player killer, String cause) {
        this.arena = arena;
        this.victim = victim;
        this.victimRole = victimRole;
        this.killer = killer;
        this.cause = cause;
    }

    public String getArena() {
        return arena;
    }

    public Player getVictim() {
        return victim;
    }

    public Role getVictimRole() {
        return victimRole;
    }

    public Player getKiller() {
        return killer;
    }

    public String getCause() {
        return cause;
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
