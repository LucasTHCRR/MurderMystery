package com.lucas.murder.util;

import java.util.HashMap;
import java.util.Map;

public final class Ph {

    private Ph() {
    }

    public static Map<String, String> none() {
        return Map.of();
    }

    public static Map<String, String> of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders need an even number of arguments");
        }
        Map<String, String> map = new HashMap<>(Math.max(4, pairs.length));
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1] == null ? "" : String.valueOf(pairs[i + 1]));
        }
        return map;
    }

    public static Map<String, String> plus(Map<String, String> base, Object... pairs) {
        Map<String, String> map = new HashMap<>(base);
        map.putAll(of(pairs));
        return map;
    }
}
