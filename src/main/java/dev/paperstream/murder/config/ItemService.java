package dev.paperstream.murder.config;

import dev.paperstream.murder.util.Items;
import dev.paperstream.murder.util.Tags;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class ItemService {

    private final ConfigManager configs;

    public ItemService(ConfigManager configs) {
        this.configs = configs;
    }

    public ConfigurationSection section(String path) {
        return configs.items().getConfigurationSection(path);
    }

    public int slot(String path, int def) {
        ConfigurationSection section = section(path);
        return section == null ? def : section.getInt("slot", def);
    }

    public ItemStack build(String path, Material fallback) {
        return build(path, fallback, Map.of());
    }

    public ItemStack build(String path, Material fallback, Map<String, String> placeholders) {
        return Items.fromSection(section(path), fallback, placeholders);
    }

    public ItemStack knife(Material skin) {
        ItemStack stack = build("knife", Material.IRON_SWORD);
        if (skin != null) {
            stack.setType(skin);
        }
        return Tags.tag(stack, Tags.ROLE_ITEM, "knife");
    }

    public ItemStack bow(Material skin) {
        ItemStack stack = build("bow", Material.BOW);
        if (skin != null) {
            stack.setType(skin);
        }
        return Tags.tag(stack, Tags.ROLE_ITEM, "bow");
    }

    public ItemStack arrow() {
        return build("arrow", Material.ARROW);
    }

    public ItemStack wand() {
        return Tags.tag(build("wand", Material.GOLDEN_AXE), Tags.WAND, "1");
    }
}
