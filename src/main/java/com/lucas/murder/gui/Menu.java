package com.lucas.murder.gui;

import com.lucas.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Menu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    public Menu(String title, int rows) {
        this.inventory = Bukkit.createInventory(this, Math.max(9, Math.min(54, rows * 9)), Text.of(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void set(int slot, ItemStack stack, Consumer<Player> action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, stack);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public void click(Player player, int slot) {
        Consumer<Player> action = actions.get(slot);
        if (action != null) {
            action.accept(player);
        }
    }

    public void fill(ConfigurationSection filler) {
        if (filler == null) {
            return;
        }
        Material material = com.lucas.murder.util.Registries.material(filler.getString("material"));
        if (material == null) {
            return;
        }
        ItemStack stack = com.lucas.murder.util.Items.of(material)
                .name(filler.getString("name", " ")).build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, stack);
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
