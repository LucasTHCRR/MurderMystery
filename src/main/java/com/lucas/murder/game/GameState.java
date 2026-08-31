package com.lucas.murder.game;

public enum GameState {

    WAITING("state-waiting"),

    COUNTDOWN("state-countdown"),

    RUNNING("state-running"),

    ENDING("state-ending"),

    DISABLED("state-disabled");

    private final String messageKey;

    GameState(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }

    public boolean joinable() {
        return this == WAITING || this == COUNTDOWN;
    }

    public boolean inGame() {
        return this != DISABLED;
    }
}
