package com.lucas.murder.gui;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GameState;
import com.lucas.murder.stats.PlayerStats;
import com.lucas.murder.util.Items;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Registries;
import com.lucas.murder.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Menus {

    private final MurderPlugin plugin;

    public Menus(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection gui(String path) {
        return plugin.configs().gui().getConfigurationSection(path);
    }

    public void openShop(Player player) {
        if (!plugin.settings().feature("cosmetics")) {
            plugin.msg().send(player, "feature-disabled");
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena != null && arena.state() == GameState.RUNNING) {
            plugin.msg().send(player, "shop-closed-ingame");
            return;
        }
        ConfigurationSection section = gui("shop");
        if (section == null) {
            return;
        }
        PlayerStats stats = plugin.stats().get(player.getUniqueId());
        Menu menu = new Menu(Text.fill(section.getString("title", "&8Shop"),
                Ph.of("player", player.getName())), section.getInt("rows", 6));

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection entry = items.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                Material material = Registries.material(entry.getString("material"));
                ItemStack stack = Items.of(material == null ? Material.PAPER : material)
                        .name(entry.getString("name", key))
                        .lore(List.of(Text.fill(plugin.msg().rawString("shop-balance"),
                                Ph.of("amount", stats.coins()))))
                        .build();
                String action = entry.getString("action", "NONE");
                menu.set(entry.getInt("slot"), stack, clicker -> runAction(clicker, action));
            }
        }
        menu.fill(section.getConfigurationSection("filler"));
        menu.open(player);
    }

    public void openCategory(Player player, String category) {
        ConfigurationSection categorySection = plugin.configs().cosmetics()
                .getConfigurationSection("categories." + category);
        ConfigurationSection layout = gui("category");
        if (categorySection == null || layout == null) {
            return;
        }
        PlayerStats stats = plugin.stats().get(player.getUniqueId());
        Menu menu = new Menu(Text.fill(layout.getString("title", "&8%category%"),
                Ph.of("category", Text.plain(categorySection.getString("title", category)))),
                layout.getInt("rows", 6));

        ConfigurationSection items = categorySection.getConfigurationSection("items");
        int slot = layout.getInt("start-slot", 10);
        if (items != null) {
            for (String id : items.getKeys(false)) {
                ConfigurationSection entry = items.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                boolean owned = stats.owns(id) || entry.getInt("price", 0) <= 0;
                boolean equipped = id.equals(stats.equipped(category));
                Material material = Registries.material(entry.getString("material"));
                List<String> lore = new ArrayList<>();
                lore.add(Text.fill(plugin.msg().rawString("shop-balance"), Ph.of("amount", stats.coins())));
                lore.add(owned ? (equipped ? "&a> equipped" : "&7Click to equip")
                        : "&7Price: &e" + entry.getInt("price", 0));
                ItemStack stack = Items.of(material == null ? Material.NAME_TAG : material)
                        .name(entry.getString("name", id))
                        .lore(lore)
                        .glowing(equipped)
                        .modelData(entry.getInt("custom-model-data", 0))
                        .build();
                String action = (owned ? "EQUIP:" : "BUY:") + category + "/" + id;
                menu.set(slot, stack, clicker -> runAction(clicker, action));
                slot++;
                if (slot % 9 == 8) {
                    slot += 2;
                }
            }
        }
        ConfigurationSection back = layout.getConfigurationSection("back");
        if (back != null) {
            Material material = Registries.material(back.getString("material"));
            menu.set(back.getInt("slot", 49),
                    Items.of(material == null ? Material.ARROW : material)
                            .name(back.getString("name", "&7Back")).build(),
                    clicker -> runAction(clicker, back.getString("action", "OPEN:shop")));
        }
        menu.fill(layout.getConfigurationSection("filler"));
        menu.open(player);
    }

    public void openVote(Player player) {
        if (!plugin.settings().feature("map-voting")) {
            plugin.msg().send(player, "feature-disabled");
            return;
        }
        ConfigurationSection section = gui("vote");
        if (section == null) {
            return;
        }
        Menu menu = new Menu(section.getString("title", "&8Vote"), section.getInt("rows", 3));
        int slot = section.getInt("start-slot", 10);
        for (Arena arena : plugin.arenas().all()) {
            if (!arena.enabled()) {
                continue;
            }
            ItemStack stack = Items.of(arena.icon())
                    .name(arena.displayName())
                    .lore(List.of(Text.fill(plugin.msg().rawString("vote-entry"),
                            Ph.of("map", Text.plain(arena.displayName()),
                                    "votes", plugin.votes().votesFor(arena)))))
                    .build();
            String name = arena.name();
            menu.set(slot, stack, clicker -> runAction(clicker, "VOTE:" + name));
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }
        menu.fill(section.getConfigurationSection("filler"));
        menu.open(player);
    }

    public void openStats(Player player, PlayerStats stats) {
        ConfigurationSection section = gui("stats");
        if (section == null) {
            return;
        }
        Menu menu = new Menu(Text.fill(section.getString("title", "&8Stats"),
                Ph.of("player", stats.name())), section.getInt("rows", 3));
        menu.set(13, Items.of(Material.PLAYER_HEAD)
                .name("&f" + stats.name())
                .lore(statLines(stats))
                .build(), null);
        menu.fill(section.getConfigurationSection("filler"));
        menu.open(player);
    }

    public List<String> statLines(PlayerStats stats) {
        List<String> lines = new ArrayList<>();
        lines.add(line("stats-games", stats.games()));
        lines.add(line("stats-wins", stats.wins()));
        lines.add(line("stats-losses", stats.losses()));
        lines.add(line("stats-kills", stats.kills()));
        lines.add(line("stats-kd", stats.formattedKd()));
        lines.add(line("stats-gold", stats.goldCollected()));
        lines.add(line("stats-heroes", stats.heroes()));
        lines.add(line("stats-coins", stats.coins()));
        lines.add(line("stats-level", stats.level()));
        lines.add(line("stats-playtime", Text.duration(stats.playtimeSeconds())));
        return lines;
    }

    private String line(String nameKey, Object value) {
        return Text.fill(plugin.msg().rawString("stats-line"),
                Ph.of("name", plugin.msg().rawString(nameKey), "value", value));
    }

    public void openSpectate(Player player) {
        ConfigurationSection section = gui("spectate");
        if (section == null) {
            return;
        }
        Menu menu = new Menu(section.getString("title", "&8Spectate"), section.getInt("rows", 3));
        int slot = section.getInt("start-slot", 10);
        for (Arena arena : plugin.arenas().all()) {
            if (arena.state() != GameState.RUNNING) {
                continue;
            }
            ItemStack stack = Items.of(arena.icon())
                    .name(arena.displayName())
                    .lore(List.of(Text.fill(plugin.msg().rawString("arena-list-entry"),
                            Ph.of("arena", arena.name(),
                                    "state", plugin.msg().rawString(arena.state().messageKey()),
                                    "current", arena.playerCount(),
                                    "max", arena.maxPlayers()))))
                    .build();
            String name = arena.name();
            menu.set(slot, stack, clicker -> runAction(clicker, "COMMAND:murder spectate " + name));
            slot++;
        }
        menu.fill(section.getConfigurationSection("filler"));
        menu.open(player);
    }

    public void runAction(Player player, String action) {
        if (action == null || action.isEmpty() || "NONE".equalsIgnoreCase(action)) {
            return;
        }
        plugin.sounds().play(player, "menu-click");
        String[] parts = action.split(":", 2);
        String type = parts[0].toUpperCase(Locale.ROOT);
        String value = parts.length > 1 ? parts[1] : "";
        switch (type) {
            case "CLOSE" -> player.closeInventory();
            case "OPEN" -> {
                if ("shop".equalsIgnoreCase(value)) {
                    openShop(player);
                } else {
                    openCategory(player, value);
                }
            }
            case "BUY" -> buy(player, value);
            case "EQUIP" -> equip(player, value);
            case "JOIN" -> {
                player.closeInventory();
                player.performCommand("murder join " + value);
            }
            case "VOTE" -> {
                player.closeInventory();
                player.performCommand("murder vote " + value);
            }
            case "COMMAND" -> {
                player.closeInventory();
                player.performCommand(value);
            }
            default -> {
            }
        }
    }

    private void buy(Player player, String value) {
        String[] parts = value.split("/", 2);
        if (parts.length < 2) {
            return;
        }
        ConfigurationSection entry = plugin.configs().cosmetics()
                .getConfigurationSection("categories." + parts[0] + ".items." + parts[1]);
        if (entry == null) {
            return;
        }
        PlayerStats stats = plugin.stats().get(player.getUniqueId());
        String permission = entry.getString("permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            plugin.msg().send(player, "shop-locked");
            plugin.sounds().play(player, "shop-fail");
            return;
        }
        if (stats.owns(parts[1])) {
            plugin.msg().send(player, "shop-already-owned");
            return;
        }
        int price = entry.getInt("price", 0);
        if (!stats.spendCoins(price)) {
            plugin.msg().send(player, "shop-not-enough", Ph.of("needed", price, "amount", stats.coins()));
            plugin.sounds().play(player, "shop-fail");
            return;
        }
        stats.addOwned(parts[1]);
        stats.equip(parts[0], parts[1]);
        plugin.msg().send(player, "shop-bought", Ph.of("item", Text.plain(entry.getString("name", parts[1]))));
        plugin.sounds().play(player, "shop-buy");
        openCategory(player, parts[0]);
    }

    private void equip(Player player, String value) {
        String[] parts = value.split("/", 2);
        if (parts.length < 2) {
            return;
        }
        ConfigurationSection entry = plugin.configs().cosmetics()
                .getConfigurationSection("categories." + parts[0] + ".items." + parts[1]);
        if (entry == null) {
            return;
        }
        String permission = entry.getString("permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            plugin.msg().send(player, "shop-locked");
            return;
        }
        plugin.stats().get(player.getUniqueId()).equip(parts[0], parts[1]);
        plugin.msg().send(player, "shop-equipped", Ph.of("item", Text.plain(entry.getString("name", parts[1]))));
        openCategory(player, parts[0]);
    }
}
