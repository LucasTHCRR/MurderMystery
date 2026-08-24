package com.lucas.murder.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class Tags {

    public static NamespacedKey ENTITY;

    public static NamespacedKey KIND;

    public static NamespacedKey WAND;

    public static NamespacedKey POTION_COLOR;

    public static NamespacedKey ROLE_ITEM;

    public static NamespacedKey GOLD_INDEX;

    public static NamespacedKey NPC_ID;

    public static final String KIND_GOLD = "gold";
    public static final String KIND_ARROW = "arrow";
    public static final String KIND_KNIFE = "knife";
    public static final String KIND_BOW = "bow";
    public static final String KIND_CORPSE = "corpse";
    public static final String KIND_HOLOGRAM = "hologram";
    public static final String KIND_NPC = "npc";
    public static final String KIND_SPIDER = "kali_spider";

    private Tags() {
    }

    public static void init(Plugin plugin) {
        ENTITY = new NamespacedKey(plugin, "murder-entity");
        KIND = new NamespacedKey(plugin, "murder-kind");
        WAND = new NamespacedKey(plugin, "murder-wand");
        POTION_COLOR = new NamespacedKey(plugin, "murder-potion");
        ROLE_ITEM = new NamespacedKey(plugin, "murder-role-item");
        GOLD_INDEX = new NamespacedKey(plugin, "murder-gold-index");
        NPC_ID = new NamespacedKey(plugin, "murder-npc-id");
    }

    public static void tag(Entity entity, String arena, String kind) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        data.set(ENTITY, PersistentDataType.STRING, arena == null ? "" : arena);
        data.set(KIND, PersistentDataType.STRING, kind);
    }

    public static boolean isTagged(Entity entity) {
        return entity.getPersistentDataContainer().has(ENTITY, PersistentDataType.STRING);
    }

    public static String arenaOf(Entity entity) {
        return entity.getPersistentDataContainer().get(ENTITY, PersistentDataType.STRING);
    }

    public static String kindOf(Entity entity) {
        return entity.getPersistentDataContainer().get(KIND, PersistentDataType.STRING);
    }

    public static boolean isKind(Entity entity, String kind) {
        return kind.equals(kindOf(entity));
    }

    public static ItemStack tag(ItemStack stack, NamespacedKey key, String value) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        stack.setItemMeta(meta);
        return stack;
    }

    public static String read(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean has(ItemStack stack, NamespacedKey key) {
        return read(stack, key) != null;
    }
}
