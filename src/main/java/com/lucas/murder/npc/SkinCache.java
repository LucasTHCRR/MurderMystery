package com.lucas.murder.npc;

import com.lucas.murder.MurderPlugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class SkinCache {

    private record Entry(Skin skin, long fetched) {
    }

    private final MurderPlugin plugin;
    private final Map<String, Entry> cache = new HashMap<>();

    public SkinCache(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void byName(String name, Consumer<Skin> callback) {
        long maxAge = plugin.settings().getInt(null, "npcs.skin-cache-hours", 24) * 3600_000L;
        Entry cached = cache.get(name.toLowerCase());
        if (cached != null && System.currentTimeMillis() - cached.fetched() < maxAge) {
            callback.accept(cached.skin());
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Skin skin = fetch(name);
            if (skin != null) {
                cache.put(name.toLowerCase(), new Entry(skin, System.currentTimeMillis()));
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(skin));
        });
    }

    private Skin fetch(String name) {
        try {
            String profile = read("https://api.mojang.com/users/profiles/minecraft/" + name);
            String uuid = between(profile, "\"id\":\"", "\"");
            if (uuid == null) {
                return null;
            }
            String textures = read("https://sessionserver.mojang.com/session/minecraft/profile/"
                    + uuid + "?unsigned=false");
            String value = between(textures, "\"value\":\"", "\"");
            String signature = between(textures, "\"signature\":\"", "\"");
            if (value == null) {
                return null;
            }
            return new Skin(name, value, signature);
        } catch (Exception exception) {
            plugin.getLogger().fine("Skin lookup for " + name + " failed: " + exception.getMessage());
            return null;
        }
    }

    private String read(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "MurderMystery");
        if (connection.getResponseCode() != 200) {
            return "";
        }
        try (InputStream stream = connection.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String between(String source, String prefix, String suffix) {
        int start = source.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = source.indexOf(suffix, start);
        return end < 0 ? null : source.substring(start, end);
    }

    public void clear() {
        cache.clear();
    }
}
