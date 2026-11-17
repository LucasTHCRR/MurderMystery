package dev.paperstream.murder.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Items {

    private final ItemStack stack;

    private Items(Material material, int amount) {
        this.stack = new ItemStack(material, Math.max(1, amount));
    }

    public static Items of(Material material) {
        return new Items(material, 1);
    }

    public static Items of(Material material, int amount) {
        return new Items(material, amount);
    }

    public Items name(String raw) {
        return name(raw, Map.of());
    }

    public Items name(String raw, Map<String, String> placeholders) {
        if (raw != null && !raw.isEmpty()) {
            edit(meta -> meta.displayName(Text.item(raw, placeholders)));
        }
        return this;
    }

    public Items lore(List<String> lines) {
        return lore(lines, Map.of());
    }

    public Items lore(List<String> lines, Map<String, String> placeholders) {
        if (lines != null && !lines.isEmpty()) {
            List<Component> lore = new ArrayList<>(lines.size());
            for (String line : lines) {
                lore.add(Text.item(line, placeholders));
            }
            edit(meta -> meta.lore(lore));
        }
        return this;
    }

    public Items glowing(boolean glowing) {
        if (glowing) {
            edit(meta -> {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
        }
        return this;
    }

    public Items unbreakable(boolean unbreakable) {
        if (unbreakable) {
            edit(meta -> meta.setUnbreakable(true));
        }
        return this;
    }

    public Items hideFlags(boolean hide) {
        if (hide) {
            edit(meta -> meta.addItemFlags(ItemFlag.values()));
        }
        return this;
    }

    public Items modelData(int data) {
        if (data > 0) {
            edit(meta -> meta.setCustomModelData(data));
        }
        return this;
    }

    public Items enchant(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            edit(meta -> meta.addEnchant(enchantment, level, true));
        }
        return this;
    }

    public ItemStack build() {
        return stack;
    }

    private void edit(java.util.function.Consumer<ItemMeta> consumer) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            stack.setItemMeta(meta);
        }
    }

    public static ItemStack fromSection(ConfigurationSection section, Material fallback, Map<String, String> placeholders) {
        if (section == null) {
            return new ItemStack(fallback);
        }
        Material material = Registries.material(section.getString("material"));
        if (material == null) {
            material = fallback;
        }
        Items builder = Items.of(material, section.getInt("amount", 1))
                .name(section.getString("name"), placeholders)
                .lore(section.getStringList("lore"), placeholders)
                .unbreakable(section.getBoolean("unbreakable", false))
                .hideFlags(section.getBoolean("hide-flags", false))
                .glowing(section.getBoolean("glowing", false))
                .modelData(section.getInt("custom-model-data", 0));

        ConfigurationSection enchants = section.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = enchantment(key);
                builder.enchant(enchantment, enchants.getInt(key, 1));
            }
        }
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    private static Enchantment enchantment(String name) {
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(name.toLowerCase(java.util.Locale.ROOT));
        if (key != null) {
            Enchantment byKey = org.bukkit.Registry.ENCHANTMENT.get(key);
            if (byKey != null) {
                return byKey;
            }
        }
        return Enchantment.getByName(name.toUpperCase(java.util.Locale.ROOT));
    }
}
