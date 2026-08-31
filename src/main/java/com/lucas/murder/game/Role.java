package com.lucas.murder.game;

import java.util.Locale;

public enum Role {

    INNOCENT("role.innocent"),
    DETECTIVE("role.detective"),
    MURDERER("role.murderer");

    private final String messageKey;

    Role(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isInnocentSide() {
        return this != MURDERER;
    }

    public static Role parse(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String names() {
        return "INNOCENT, DETECTIVE, MURDERER";
    }
}
