package com.lucas.murder.hook;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.hook.corpse.CorpseRebornProvider;
import com.lucas.murder.hook.corpse.InternalCorpseProvider;
import com.lucas.murder.util.Registries;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CorpseProviderRegistry {

    private final MurderPlugin plugin;
    private final Map<String, CorpseProvider> providers = new LinkedHashMap<>();
    private final List<Object> roundCorpses = new ArrayList<>();
    private CorpseProvider active;

    public CorpseProviderRegistry(MurderPlugin plugin) {
        this.plugin = plugin;
        providers.put("CORPSEREBORN", new CorpseRebornProvider(plugin));
        providers.put("INTERNAL", new InternalCorpseProvider(plugin));
    }

    public void detect() {
        String configured = plugin.settings().getString(null, "corpses.provider", "AUTO");
        if (!"AUTO".equalsIgnoreCase(configured)) {
            CorpseProvider provider = providers.get(configured.toUpperCase(Locale.ROOT));
            if (provider != null && provider.isAvailable()) {
                active = provider;
                plugin.getLogger().info("Corpse provider: " + provider.name());
                return;
            }
            plugin.getLogger().warning("Corpse provider " + configured
                    + " is not available - falling back to the internal one.");
        } else {
            for (String name : plugin.settings().getStringList(null, "corpses.provider-priority")) {
                CorpseProvider provider = providers.get(name.toUpperCase(Locale.ROOT));
                if (provider != null && provider.isAvailable()) {
                    active = provider;
                    plugin.getLogger().info("Corpse provider: " + provider.name());
                    return;
                }
            }
        }
        active = providers.get("INTERNAL");
    }

    public CorpseProvider active() {
        if (active == null) {
            detect();
        }
        return active;
    }

    public boolean switchTo(String name) {
        CorpseProvider provider = providers.get(name.toUpperCase(Locale.ROOT));
        if (provider == null || !provider.isAvailable()) {
            return false;
        }
        active = provider;
        return true;
    }

    public Map<String, CorpseProvider> providers() {
        return providers;
    }

    public String names() {
        return String.join(", ", providers.keySet());
    }

    public void spawn(Arena arena, GamePlayer victim, Location location) {
        if (!plugin.settings().getBoolean(arena, "corpses.enabled", true)) {
            return;
        }
        Player player = victim.player();
        bloodAndSound(arena, location);
        if (player == null) {
            return;
        }
        int seconds = plugin.settings().getInt(arena, "corpses.seconds", 30);
        try {
            Object handle = active().spawn(player, location, seconds);
            if (handle != null) {
                roundCorpses.add(handle);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Corpse could not be spawned: " + throwable.getMessage());
        }
    }

    private void bloodAndSound(Arena arena, Location location) {
        if (location.getWorld() == null) {
            return;
        }
        if (plugin.settings().getBoolean(arena, "corpses.blood.enabled", true)) {
            Particle particle = Registries.particle(
                    plugin.settings().getString(arena, "corpses.blood.particle", "DUST"));
            int amount = plugin.settings().getInt(arena, "corpses.blood.amount", 30);
            if (particle == Particle.DUST) {
                Color color = Registries.color(
                        plugin.settings().getString(arena, "corpses.blood.color", "#8B0000"));
                location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 1, 0),
                        amount, 0.3, 0.5, 0.3,
                        new Particle.DustOptions(color == null ? Color.RED : color, 1.2F));
            } else if (particle != null) {
                location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0),
                        amount, 0.3, 0.5, 0.3, 0);
            }
        }
        if (plugin.settings().getBoolean(arena, "corpses.sound.enabled", true)) {
            var sound = Registries.sound(
                    plugin.settings().getString(arena, "corpses.sound.name", "ENTITY_PLAYER_BIG_FALL"));
            if (sound != null) {
                location.getWorld().playSound(location, sound,
                        (float) plugin.settings().getDouble(arena, "corpses.sound.volume", 1.0),
                        (float) plugin.settings().getDouble(arena, "corpses.sound.pitch", 0.8));
            }
        }
    }

    public void removeAll() {
        try {
            active().removeAll();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Corpses could not be removed: " + throwable.getMessage());
        }
        roundCorpses.clear();
    }
}
