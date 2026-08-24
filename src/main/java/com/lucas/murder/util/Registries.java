package com.lucas.murder.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Registries {

    private static final Map<String, Sound> SOUNDS = new HashMap<>();
    private static final Map<String, String> LEGACY_PARTICLES = new HashMap<>();

    static {
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey key = sound.getKey();
            SOUNDS.put(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_'), sound);
            SOUNDS.put(key.toString().toUpperCase(Locale.ROOT), sound);
        }
        LEGACY_PARTICLES.put("REDSTONE", "DUST");
        LEGACY_PARTICLES.put("SPELL_MOB", "ENTITY_EFFECT");
        LEGACY_PARTICLES.put("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        LEGACY_PARTICLES.put("VILLAGER_ANGRY", "ANGRY_VILLAGER");
        LEGACY_PARTICLES.put("SMOKE_NORMAL", "SMOKE");
        LEGACY_PARTICLES.put("SMOKE_LARGE", "LARGE_SMOKE");
        LEGACY_PARTICLES.put("EXPLOSION_NORMAL", "EXPLOSION");
        LEGACY_PARTICLES.put("EXPLOSION_LARGE", "EXPLOSION_EMITTER");
    }

    private Registries() {
    }

    public static Sound sound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return SOUNDS.get(name.trim().toUpperCase(Locale.ROOT));
    }

    public static Material material(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(name.trim());
        return material == null || material.isAir() ? null : material;
    }

    public static Particle particle(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String upper = name.trim().toUpperCase(Locale.ROOT);
        upper = LEGACY_PARTICLES.getOrDefault(upper, upper);
        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static PotionEffectType effect(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleaned = name.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(cleaned);
        if (key != null) {
            PotionEffectType byKey = Registry.EFFECT.get(key);
            if (byKey != null) {
                return byKey;
            }
        }
        try {
            return PotionEffectType.getByName(name.trim());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static EntityType entityType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return EntityType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static org.bukkit.Color color(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        String cleaned = hex.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        try {
            return org.bukkit.Color.fromRGB(Integer.parseInt(cleaned, 16));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
