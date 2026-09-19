package com.lucas.murder.extras.kali;

import org.bukkit.configuration.ConfigurationSection;

public final class KaliEffect {

    private final String id;
    private final String type;
    private final String name;
    private final boolean blessing;
    private final ConfigurationSection config;
    private int secondsLeft;
    private int counter;

    public KaliEffect(String id, String type, String name, boolean blessing,
                      ConfigurationSection config, int secondsLeft) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.blessing = blessing;
        this.config = config;
        this.secondsLeft = secondsLeft;
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    public boolean blessing() {
        return blessing;
    }

    public ConfigurationSection config() {
        return config;
    }

    public boolean tick() {
        counter++;
        if (secondsLeft <= 0) {
            return false;
        }
        secondsLeft--;
        return secondsLeft <= 0;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    public int counter() {
        return counter;
    }

    public void resetCounter() {
        counter = 0;
    }
}
