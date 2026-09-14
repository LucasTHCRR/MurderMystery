package com.lucas.murder.npc;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.hook.NpcProvider;
import com.lucas.murder.stats.Leaderboard;
import com.lucas.murder.util.Text;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NpcManager {

    private final MurderPlugin plugin;
    private final Map<Integer, MurderNpc> npcs = new LinkedHashMap<>();
    private final Map<Integer, Hologram> holograms = new LinkedHashMap<>();
    private final SkinCache skins;
    private int nextId = 1;
    private int tick;

    public NpcManager(MurderPlugin plugin) {
        this.plugin = plugin;
        this.skins = new SkinCache(plugin);
    }

    public SkinCache skins() {
        return skins;
    }

    public Collection<MurderNpc> all() {
        return npcs.values();
    }

    public MurderNpc get(int id) {
        return npcs.get(id);
    }

    public void load() {
        despawnAll();
        npcs.clear();
        ConfigurationSection root = plugin.configs().npcs();
        nextId = Math.max(1, root.getInt("next-id", 1));
        ConfigurationSection section = root.getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            MurderNpc npc = MurderNpc.load(id, section.getConfigurationSection(key));
            if (npc != null) {
                npcs.put(id, npc);
                nextId = Math.max(nextId, id + 1);
            }
        }
    }

    public void save() {
        ConfigurationSection root = plugin.configs().npcs();
        root.set("next-id", nextId);
        root.set("npcs", null);
        ConfigurationSection section = root.createSection("npcs");
        for (MurderNpc npc : npcs.values()) {
            npc.save(section.createSection(String.valueOf(npc.id())));
        }
        plugin.configs().save("npcs.yml");
    }

    public MurderNpc create(NpcType type, Location location, String arena) {
        MurderNpc npc = new MurderNpc(nextId++, type, location);
        npc.arena(arena);
        npcs.put(npc.id(), npc);
        spawn(npc);
        save();
        return npc;
    }

    public boolean remove(int id) {
        MurderNpc npc = npcs.remove(id);
        if (npc == null) {
            return false;
        }
        despawn(npc);
        save();
        return true;
    }

    public void spawnAll() {
        if (!plugin.settings().feature("npcs")
                || !plugin.settings().getBoolean(null, "npcs.enabled", true)) {
            return;
        }
        for (MurderNpc npc : npcs.values()) {
            spawn(npc);
        }
    }

    public void spawn(MurderNpc npc) {
        if (!plugin.settings().getBoolean(null, "npcs.enabled", true)) {
            return;
        }
        NpcProvider provider = plugin.npcProviders().active();
        try {
            provider.spawn(npc);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("NPC #" + npc.id() + " could not be spawned: " + throwable.getMessage());
        }
        updateHologram(npc);
    }

    public void despawn(MurderNpc npc) {
        try {
            plugin.npcProviders().active().despawn(npc);
        } catch (Throwable ignored) {

        }
        Hologram hologram = holograms.remove(npc.id());
        if (hologram != null) {
            hologram.remove();
        }
    }

    public void despawnAll() {
        for (MurderNpc npc : npcs.values()) {
            despawn(npc);
        }
        for (Hologram hologram : holograms.values()) {
            hologram.remove();
        }
        holograms.clear();
    }

    public int respawnAll() {
        despawnAll();
        spawnAll();
        return npcs.size();
    }

    public void tick() {
        tick++;
        int interval = Math.max(1, plugin.settings().getInt(null, "npcs.hologram-update-seconds", 1));
        if (tick % interval != 0) {
            return;
        }
        for (MurderNpc npc : npcs.values()) {
            updateHologram(npc);
            lookAtPlayers(npc);
        }
    }

    private void updateHologram(MurderNpc npc) {
        if (npc.hologram().isEmpty()) {
            Hologram existing = holograms.remove(npc.id());
            if (existing != null) {
                existing.remove();
            }
            return;
        }
        Location location = npc.location();
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String raw : npc.hologram()) {
            lines.add(resolve(raw, npc));
        }
        holograms.computeIfAbsent(npc.id(), key -> new Hologram(plugin)).render(location, lines);
    }

    private String resolve(String raw, MurderNpc npc) {
        Arena arena = npc.arena() == null ? null : plugin.arenas().get(npc.arena());
        String text = plugin.boards().placeholders().apply(raw, arena, null, null);
        if (npc.type() == NpcType.LEADERBOARD && text.contains("%top_")) {
            List<Leaderboard.Entry> top = plugin.stats().leaderboard().top(npc.stat(), 10);
            for (int i = 1; i <= 10; i++) {
                Leaderboard.Entry entry = i <= top.size() ? top.get(i - 1) : null;
                text = text.replace("%top_" + i + "_name%", entry == null ? "-" : entry.name())
                        .replace("%top_" + i + "_value%", entry == null ? "-" : String.valueOf(entry.value()));
            }
        }
        return text;
    }

    private void lookAtPlayers(MurderNpc npc) {
        if (!npc.lookAtPlayers() || !plugin.settings().getBoolean(null, "npcs.look-at-players", true)) {
            return;
        }
        if (plugin.npcProviders().active().supports(NpcProvider.Feature.LOOK_AT_PLAYER)) {
            return;
        }
        Location location = npc.location();
        if (location == null || location.getWorld() == null) {
            return;
        }
        double range = plugin.settings().getInt(null, "npcs.look-range", 10);
        Player closest = null;
        double best = range * range;
        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                closest = player;
            }
        }
        if (closest == null || !(npc.handle() instanceof Entity entity)) {
            return;
        }
        Location facing = location.clone();
        facing.setDirection(closest.getLocation().toVector().subtract(location.toVector()));
        entity.setRotation(facing.getYaw(), 0F);
    }

    public MurderNpc byEntity(Entity entity) {
        Integer id = plugin.npcProviders().active().npcIdOf(entity);
        if (id != null) {
            return npcs.get(id);
        }
        return null;
    }

    public String describe(MurderNpc npc) {
        String extra = npc.arena() != null ? npc.arena() : "";
        if (npc.type() == NpcType.LEADERBOARD) {
            extra = npc.stat();
        }
        return Text.plain(extra);
    }
}
