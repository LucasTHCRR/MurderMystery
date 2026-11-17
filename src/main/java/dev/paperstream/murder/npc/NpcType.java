package dev.paperstream.murder.npc;

import java.util.Locale;

public enum NpcType {

    JOIN(true),

    RANDOM(false),

    VOTE(false),

    SHOP(false),

    STATS(false),

    LEADERBOARD(false),

    SPECTATE(false),

    LEAVE(false),

    INFO(false),

    REPLAY(false),

    KALI(true),

    POTION(true),

    CORPSE(false);

    private final boolean needsArena;

    NpcType(boolean needsArena) {
        this.needsArena = needsArena;
    }

    public boolean needsArena() {
        return needsArena;
    }

    public static NpcType parse(String name) {
        if (name == null) {
            return null;
        }
        try {
            NpcType type = valueOf(name.trim().toUpperCase(Locale.ROOT));
            return type == CORPSE ? null : type;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String names() {
        StringBuilder builder = new StringBuilder();
        for (NpcType type : values()) {
            if (type == CORPSE) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(type.name());
        }
        return builder.toString();
    }
}
