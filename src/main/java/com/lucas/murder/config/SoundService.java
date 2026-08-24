package com.lucas.murder.config;

import com.lucas.murder.util.Registries;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class SoundService {

    private final ConfigManager configs;

    public SoundService(ConfigManager configs) {
        this.configs = configs;
    }

    public void play(Player player, String key) {
        if (player == null) {
            return;
        }
        ConfigurationSection section = configs.sounds().getConfigurationSection(key);
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        Sound sound = Registries.sound(section.getString("sound"));
        if (sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound,
                (float) section.getDouble("volume", 1.0D),
                (float) section.getDouble("pitch", 1.0D));
    }

    public void play(Collection<? extends Player> players, String key) {
        for (Player player : players) {
            play(player, key);
        }
    }

    public void playAt(Location location, String key) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        ConfigurationSection section = configs.sounds().getConfigurationSection(key);
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        Sound sound = Registries.sound(section.getString("sound"));
        if (sound == null) {
            return;
        }
        location.getWorld().playSound(location, sound,
                (float) section.getDouble("volume", 1.0D),
                (float) section.getDouble("pitch", 1.0D));
    }

    public void playRaw(Player player, String soundName, float volume, float pitch) {
        Sound sound = Registries.sound(soundName);
        if (sound != null && player != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
