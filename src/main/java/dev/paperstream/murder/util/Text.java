package dev.paperstream.murder.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    public static String fill(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return raw == null ? "" : raw;
        }
        String out = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace('%' + entry.getKey() + '%', entry.getValue() == null ? "" : entry.getValue());
        }
        return out;
    }

    public static Component of(String raw) {
        return raw == null ? Component.empty() : LEGACY.deserialize(raw);
    }

    public static Component of(String raw, Map<String, String> placeholders) {
        return of(fill(raw, placeholders));
    }

    public static Component item(String raw) {
        return of(raw).decoration(TextDecoration.ITALIC, false);
    }

    public static Component item(String raw, Map<String, String> placeholders) {
        return of(raw, placeholders).decoration(TextDecoration.ITALIC, false);
    }

    public static String plain(String raw) {
        return PlainTextComponentSerializer.plainText().serialize(of(raw));
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    public static String clock(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public static String duration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m " + rest + "s";
    }
}
