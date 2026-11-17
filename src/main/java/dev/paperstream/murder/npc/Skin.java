package dev.paperstream.murder.npc;

public record Skin(String owner, String value, String signature) {

    public boolean valid() {
        return value != null && !value.isEmpty();
    }

    public static Skin empty() {
        return new Skin(null, null, null);
    }
}
