package dev.paperstream.murder.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Cooldowns {

    private final Map<String, Long> expiry = new HashMap<>();

    private static String key(UUID player, String what) {
        return player + "|" + what;
    }

    public void set(UUID player, String what, long millis) {
        if (millis <= 0) {
            expiry.remove(key(player, what));
            return;
        }
        expiry.put(key(player, what), System.currentTimeMillis() + millis);
    }

    public void setSeconds(UUID player, String what, double seconds) {
        set(player, what, (long) (seconds * 1000D));
    }

    public boolean isActive(UUID player, String what) {
        return remaining(player, what) > 0;
    }

    public long remaining(UUID player, String what) {
        Long until = expiry.get(key(player, what));
        if (until == null) {
            return 0L;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            expiry.remove(key(player, what));
            return 0L;
        }
        return left;
    }

    public int remainingSeconds(UUID player, String what) {
        return (int) Math.ceil(remaining(player, what) / 1000D);
    }

    public boolean tryUse(UUID player, String what, long millis) {
        if (isActive(player, what)) {
            return false;
        }
        set(player, what, millis);
        return true;
    }

    public void clear(UUID player) {
        expiry.keySet().removeIf(key -> key.startsWith(player.toString() + "|"));
    }

    public void clear(UUID player, String what) {
        expiry.remove(key(player, what));
    }

    public void clearAll() {
        expiry.clear();
    }
}
