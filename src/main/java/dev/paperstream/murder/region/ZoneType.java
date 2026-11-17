package dev.paperstream.murder.region;

import java.util.Locale;

public enum ZoneType {

    INSTANT,

    DAMAGE,

    PUSHBACK,

    BORDER,

    NO_BUILD,

    SAFE;

    public static ZoneType parse(String name) {
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
        StringBuilder builder = new StringBuilder();
        for (ZoneType type : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(type.name());
        }
        return builder.toString();
    }
}
