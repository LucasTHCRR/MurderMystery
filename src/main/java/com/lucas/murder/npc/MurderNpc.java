package com.lucas.murder.npc;

import com.lucas.murder.util.Locations;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MurderNpc {

    public enum Pose {
        STANDING, SITTING, SNEAKING, LYING;

        public static Pose parse(String name) {
            if (name == null) {
                return STANDING;
            }
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return STANDING;
            }
        }
    }

    public enum EquipSlot {
        HAND, OFFHAND, HEAD, CHEST, LEGS, FEET;

        public static EquipSlot parse(String name) {
            if (name == null) {
                return null;
            }
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        public static String names() {
            return "hand, offhand, head, chest, legs, feet";
        }
    }

    private final int id;
    private NpcType type;
    private String arena;
    private String name = "";
    private Skin skin = Skin.empty();
    private Location location;
    private Pose pose = Pose.STANDING;
    private boolean lookAtPlayers = true;
    private String stat = "wins";
    private final Map<EquipSlot, Material> equipment = new LinkedHashMap<>();
    private final List<String> hologram = new ArrayList<>();

    private Object handle;

    public MurderNpc(int id, NpcType type, Location location) {
        this.id = id;
        this.type = type;
        this.location = location;
    }

    public static MurderNpc load(int id, ConfigurationSection section) {
        NpcType type = NpcType.parse(section.getString("type"));
        if (type == null) {
            type = NpcType.JOIN;
        }
        Location location = Locations.fromMap(section.get("location"));
        if (location == null) {
            return null;
        }
        MurderNpc npc = new MurderNpc(id, type, location);
        npc.arena = section.getString("arena");
        npc.name = section.getString("name", "");
        npc.pose = Pose.parse(section.getString("pose"));
        npc.lookAtPlayers = section.getBoolean("look-at-players", true);
        npc.stat = section.getString("stat", "wins");
        ConfigurationSection skinSection = section.getConfigurationSection("skin");
        if (skinSection != null) {
            npc.skin = new Skin(skinSection.getString("owner"),
                    skinSection.getString("value"),
                    skinSection.getString("signature"));
        }
        ConfigurationSection equip = section.getConfigurationSection("equipment");
        if (equip != null) {
            for (String key : equip.getKeys(false)) {
                EquipSlot slot = EquipSlot.parse(key);
                Material material = com.lucas.murder.util.Registries.material(equip.getString(key));
                if (slot != null && material != null) {
                    npc.equipment.put(slot, material);
                }
            }
        }
        npc.hologram.addAll(section.getStringList("hologram"));
        return npc;
    }

    public void save(ConfigurationSection section) {
        section.set("type", type.name());
        section.set("arena", arena);
        section.set("name", name);
        section.set("pose", pose.name());
        section.set("look-at-players", lookAtPlayers);
        section.set("stat", type == NpcType.LEADERBOARD ? stat : null);
        section.set("location", Locations.toMap(location));
        if (skin.valid() || skin.owner() != null) {
            ConfigurationSection skinSection = section.createSection("skin");
            skinSection.set("owner", skin.owner());
            skinSection.set("value", skin.value());
            skinSection.set("signature", skin.signature());
        } else {
            section.set("skin", null);
        }
        section.set("equipment", null);
        if (!equipment.isEmpty()) {
            ConfigurationSection equip = section.createSection("equipment");
            for (Map.Entry<EquipSlot, Material> entry : equipment.entrySet()) {
                equip.set(entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue().name());
            }
        }
        section.set("hologram", hologram.isEmpty() ? null : hologram);
    }

    public int id() {
        return id;
    }

    public NpcType type() {
        return type;
    }

    public void type(NpcType type) {
        this.type = type;
    }

    public String arena() {
        return arena;
    }

    public void arena(String arena) {
        this.arena = arena;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public Skin skin() {
        return skin;
    }

    public void skin(Skin skin) {
        this.skin = skin;
    }

    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;
    }

    public Pose pose() {
        return pose;
    }

    public void pose(Pose pose) {
        this.pose = pose;
    }

    public boolean lookAtPlayers() {
        return lookAtPlayers;
    }

    public void lookAtPlayers(boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    public String stat() {
        return stat;
    }

    public void stat(String stat) {
        this.stat = stat;
    }

    public Map<EquipSlot, Material> equipment() {
        return equipment;
    }

    public List<String> hologram() {
        return hologram;
    }

    public Object handle() {
        return handle;
    }

    public void handle(Object handle) {
        this.handle = handle;
    }
}
