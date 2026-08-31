package com.lucas.murder.game;

public final class WinChecker {

    public enum Result {

        NONE,
        INNOCENTS,
        MURDERERS,

        DRAW,

        ABORTED
    }

    private WinChecker() {
    }

    public static Result check(Arena arena) {
        if (arena.state() != GameState.RUNNING) {
            return Result.NONE;
        }
        int aliveMurderers = 0;
        int aliveInnocents = 0;
        int participants = 0;
        for (GamePlayer participant : arena.participants()) {
            if (participant.spectator()) {
                continue;
            }
            participants++;
            if (!participant.alive()) {
                continue;
            }
            if (participant.role() == Role.MURDERER) {
                aliveMurderers++;
            } else {
                aliveInnocents++;
            }
        }
        if (participants == 0) {
            return Result.ABORTED;
        }
        if (aliveMurderers == 0 && aliveInnocents == 0) {
            return Result.DRAW;
        }
        if (aliveMurderers == 0) {
            return Result.INNOCENTS;
        }
        if (aliveInnocents == 0) {
            return Result.MURDERERS;
        }
        return Result.NONE;
    }

    public static Result onTimeOver() {
        return Result.INNOCENTS;
    }
}
