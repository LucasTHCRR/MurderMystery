package com.lucas.murder.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface CorpseProvider {

    enum Feature {
        HOLOGRAM, LOOT, ANIMATION
    }

    String name();

    boolean isAvailable();

    Object spawn(Player victim, Location location, int seconds);

    void remove(Object handle);

    void removeAll();

    boolean supports(Feature feature);
}
