package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.util.Locations;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Text;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SignManager {

    private final MurderPlugin plugin;

    private final Map<Location, String> signs = new LinkedHashMap<>();

    public SignManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        signs.clear();
        List<?> raw = plugin.configs().arenas().getList("signs", List.of());
        for (Object entry : raw) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            Location location = Locations.fromMap(map.get("location"));
            Object arena = map.get("arena");
            if (location != null && arena != null) {
                signs.put(location, String.valueOf(arena));
            }
        }
    }

    public void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Location, String> entry : signs.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("location", Locations.toMap(entry.getKey()));
            map.put("arena", entry.getValue());
            list.add(map);
        }
        plugin.configs().arenas().set("signs", list);
        plugin.configs().save("arenas.yml");
    }

    public void add(Location location, Arena arena) {
        signs.put(location.getBlock().getLocation(), arena.name());
        save();
        update(arena);
    }

    public boolean remove(Location location) {
        boolean removed = signs.remove(location.getBlock().getLocation()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Arena arenaAt(Location location) {
        String name = signs.get(location.getBlock().getLocation());
        return name == null ? null : plugin.arenas().get(name);
    }

    public void update(Arena arena) {
        if (!plugin.settings().feature("join-signs")) {
            return;
        }
        for (Map.Entry<Location, String> entry : new LinkedHashMap<>(signs).entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(arena.name())) {
                continue;
            }
            Location location = entry.getKey();
            if (location.getWorld() == null || !location.getWorld().isChunkLoaded(
                    location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            Block block = location.getBlock();
            if (!(block.getState() instanceof Sign sign)) {
                signs.remove(location);
                continue;
            }
            Map<String, String> placeholders = Ph.of(
                    "arena", Text.plain(arena.displayName()),
                    "current", arena.playerCount(),
                    "max", arena.maxPlayers(),
                    "state", plugin.msg().rawString(arena.state().messageKey()));
            for (int line = 0; line < 4; line++) {
                String raw = plugin.msg().rawString("sign.line" + (line + 1));
                sign.getSide(Side.FRONT).line(line, Text.of(Text.fill(raw, placeholders)));
            }
            sign.update(true, false);
        }
    }

    public void updateAll() {
        for (Arena arena : plugin.arenas().all()) {
            update(arena);
        }
    }
}
