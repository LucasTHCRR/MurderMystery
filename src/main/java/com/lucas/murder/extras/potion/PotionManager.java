package com.lucas.murder.extras.potion;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Registries;
import com.lucas.murder.util.Tags;
import com.lucas.murder.util.Text;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PotionManager {

    private static final Map<String, Color> COLORS = Map.ofEntries(
            Map.entry("RED", Color.RED),
            Map.entry("BLUE", Color.BLUE),
            Map.entry("GREEN", Color.GREEN),
            Map.entry("YELLOW", Color.YELLOW),
            Map.entry("PURPLE", Color.PURPLE),
            Map.entry("PINK", Color.FUCHSIA),
            Map.entry("ORANGE", Color.ORANGE),
            Map.entry("CYAN", Color.AQUA),
            Map.entry("WHITE", Color.WHITE),
            Map.entry("BLACK", Color.BLACK));

    private final MurderPlugin plugin;

    private final Map<String, Map<String, String>> tables = new HashMap<>();

    private final Map<UUID, Set<String>> knowledge = new HashMap<>();
    private final Map<UUID, Integer> bought = new HashMap<>();

    public PotionManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection root() {
        return plugin.configs().potions().getConfigurationSection("mystery-potion");
    }

    public boolean enabled() {
        ConfigurationSection root = root();
        return plugin.settings().feature("mystery-potion") && root != null && root.getBoolean("enabled", true);
    }

    public void startRound(Arena arena) {
        if (!enabled()) {
            return;
        }
        ConfigurationSection root = root();
        if (!root.getBoolean("reroll-each-round", true) && tables.containsKey(arena.name())) {
            return;
        }
        List<String> colors = new ArrayList<>(root.getStringList("colors"));
        ConfigurationSection effects = root.getConfigurationSection("effects");
        if (effects == null || colors.isEmpty()) {
            return;
        }
        List<String> good = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        for (String key : effects.getKeys(false)) {
            (effects.getConfigurationSection(key).getBoolean("good", true) ? good : bad).add(key);
        }
        Collections.shuffle(colors);
        Collections.shuffle(good);
        Collections.shuffle(bad);

        int goodRatio = root.getInt("ratio.good", 60);
        int badRatio = root.getInt("ratio.bad", 40);
        Map<String, String> table = new HashMap<>();
        int goodIndex = 0;
        int badIndex = 0;
        for (String color : colors) {
            boolean pickGood = good.size() > goodIndex
                    && (bad.size() <= badIndex
                    || Math.random() * (goodRatio + badRatio) < goodRatio);
            if (pickGood) {
                table.put(color.toUpperCase(Locale.ROOT), good.get(goodIndex++ % good.size()));
            } else if (!bad.isEmpty()) {
                table.put(color.toUpperCase(Locale.ROOT), bad.get(badIndex++ % bad.size()));
            }
        }
        tables.put(arena.name(), table);
        for (GamePlayer participant : arena.participants()) {
            knowledge.remove(participant.uuid());
            bought.remove(participant.uuid());
        }
    }

    public void tick(Arena arena) {

    }

    public void buy(Arena arena, Player player) {
        if (!enabled()) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return;
        }
        ConfigurationSection root = root();
        List<String> allowed = root.getStringList("allow-roles");
        if (!allowed.isEmpty() && participant.role() != null
                && !allowed.contains(participant.role().name())) {
            plugin.msg().send(player, "potion-role-denied");
            return;
        }
        int cooldown = root.getInt("cooldown-seconds", 5);
        if (!plugin.cooldowns().tryUse(player.getUniqueId(), "potion", cooldown * 1000L)) {
            plugin.msg().send(player, "potion-cooldown",
                    Ph.of("seconds", plugin.cooldowns().remainingSeconds(player.getUniqueId(), "potion")));
            return;
        }
        int max = root.getInt("max-per-round", 10);
        if (max > 0 && bought.getOrDefault(player.getUniqueId(), 0) >= max) {
            plugin.msg().send(player, "potion-limit");
            return;
        }
        int cost = root.getInt("cost", 1);
        if (participant.gold() < cost) {
            plugin.msg().send(player, "potion-not-enough-gold", Ph.of("needed", cost));
            return;
        }
        participant.gold(participant.gold() - cost);
        arena.gold().syncItem(participant, player);
        bought.merge(player.getUniqueId(), 1, Integer::sum);

        Map<String, String> table = tables.get(arena.name());
        if (table == null || table.isEmpty()) {
            return;
        }
        List<String> colors = new ArrayList<>(table.keySet());
        String color = colors.get((int) (Math.random() * colors.size()));
        player.getInventory().addItem(create(arena, player, color));
        plugin.msg().send(player, "potion-bought", Ph.of("cost", cost));
        plugin.sounds().play(player, "potion-buy");
    }

    public ItemStack create(Arena arena, Player player, String color) {
        ItemStack stack = new ItemStack(Material.POTION);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(COLORS.getOrDefault(color, Color.WHITE));
            potionMeta.displayName(Text.item(nameFor(arena, player, color)));
            potionMeta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            stack.setItemMeta(potionMeta);
        }
        return Tags.tag(stack, Tags.POTION_COLOR, color);
    }

    private String nameFor(Arena arena, Player player, String color) {
        if (!knows(player.getUniqueId(), arena, color)) {
            return plugin.msg().rawString("potion-unknown-name");
        }
        ConfigurationSection effect = effectOf(arena, color);
        if (effect == null) {
            return plugin.msg().rawString("potion-unknown-name");
        }
        return Text.fill(plugin.msg().rawString("potion-known-name"), Ph.of(
                "effect", pretty(effect.getString("effect", "?")),
                "duration", Text.clock(effect.getInt("duration", 0))));
    }

    private ConfigurationSection effectOf(Arena arena, String color) {
        Map<String, String> table = tables.get(arena.name());
        if (table == null) {
            return null;
        }
        String id = table.get(color);
        return id == null ? null : root().getConfigurationSection("effects." + id);
    }

    private boolean knows(UUID uuid, Arena arena, String color) {
        if ("GLOBAL".equalsIgnoreCase(root().getString("knowledge", "PLAYER"))) {
            for (Set<String> known : knowledge.values()) {
                if (known.contains(color)) {
                    return true;
                }
            }
            return false;
        }
        return knowledge.getOrDefault(uuid, Set.of()).contains(color);
    }

    public void drink(Arena arena, Player player, ItemStack stack) {
        String color = Tags.read(stack, Tags.POTION_COLOR);
        if (color == null) {
            return;
        }
        ConfigurationSection effect = effectOf(arena, color);
        if (effect == null) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        List<String> allowed = effect.getStringList("allow-roles");
        boolean blocked = !allowed.isEmpty() && participant != null && participant.role() != null
                && !allowed.contains(participant.role().name());
        if (!blocked) {
            PotionEffectType type = Registries.effect(effect.getString("effect"));
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type,
                        effect.getInt("duration", 10) * 20,
                        effect.getInt("amplifier", 0), false, true));
            }
        }
        knowledge.computeIfAbsent(player.getUniqueId(), key -> new HashSet<>()).add(color);
        plugin.msg().send(player, "potion-revealed", Ph.of(
                "color", color, "effect", pretty(effect.getString("effect", "?"))));
        plugin.sounds().play(player, "potion-reveal");

        if ("GLOBAL".equalsIgnoreCase(root().getString("knowledge", "PLAYER"))) {
            arena.broadcast("potion-revealed-global", Ph.of(
                    "player", player.getName(), "color", color,
                    "effect", pretty(effect.getString("effect", "?"))));
        }

        refreshNames(arena, player);
    }

    private void refreshNames(Arena arena, Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            String color = Tags.read(stack, Tags.POTION_COLOR);
            if (color == null) {
                continue;
            }
            inventory.setItem(slot, create(arena, player, color));
        }
    }

    private String pretty(String key) {
        String lower = key.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public void clearArena(Arena arena) {
        tables.remove(arena.name());
        for (GamePlayer participant : arena.participants()) {
            knowledge.remove(participant.uuid());
            bought.remove(participant.uuid());
        }
    }

    public void clearAll() {
        tables.clear();
        knowledge.clear();
        bought.clear();
    }

    public boolean dropAllowed() {
        return root() != null && root().getBoolean("drop-allowed", false);
    }
}
