package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Tags;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public final class GoldManager {

    private final MurderPlugin plugin;
    private final Arena arena;
    private final Map<Integer, Item> active = new HashMap<>();
    private final Map<Integer, Integer> respawn = new HashMap<>();

    public GoldManager(MurderPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void fill() {
        clear();
        for (int index = 0; index < arena.goldSpawns().size(); index++) {
            spawn(index);
        }
    }

    private void spawn(int index) {
        Location location = arena.goldSpawns().get(index);
        if (location == null || location.getWorld() == null) {
            return;
        }
        ItemStack stack = plugin.items().build("gold", Material.GOLD_INGOT);
        Item item = location.getWorld().dropItem(location.clone().add(0, 0.2D, 0), stack);
        item.setVelocity(item.getVelocity().zero());
        item.setGravity(false);
        item.setCanMobPickup(false);
        item.setPickupDelay(0);
        item.setInvulnerable(true);
        Tags.tag(item, arena.name(), Tags.KIND_GOLD);
        item.getPersistentDataContainer().set(Tags.GOLD_INDEX, PersistentDataType.INTEGER, index);
        active.put(index, item);
        respawn.remove(index);
    }

    public void tick() {
        int seconds = plugin.settings().getInt(arena, "gold.respawn-seconds", 20);
        for (Map.Entry<Integer, Integer> entry : new HashMap<>(respawn).entrySet()) {
            int left = entry.getValue() - 1;
            if (left <= 0) {
                spawn(entry.getKey());
            } else {
                respawn.put(entry.getKey(), left);
            }
        }

        for (Item item : active.values()) {
            if (item != null && item.isValid()) {
                item.setTicksLived(1);
            }
        }
        if (seconds <= 0) {
            respawn.clear();
        }
    }

    public boolean collect(Player player, Item item) {
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return true;
        }
        Integer index = item.getPersistentDataContainer().get(Tags.GOLD_INDEX, PersistentDataType.INTEGER);

        if (participant.goldBlocked()) {
            return true;
        }
        if (participant.role() == Role.MURDERER
                && !plugin.settings().getBoolean(arena, "gold.murderer-can-collect", true)) {
            return true;
        }
        if (plugin.settings().getBoolean(arena, "gold.require-movement", true)
                && System.currentTimeMillis() - participant.lastMove() > 5000L) {
            return true;
        }

        item.remove();
        if (index != null) {
            active.remove(index);
            respawn.put(index, Math.max(1, plugin.settings().getInt(arena, "gold.respawn-seconds", 20)));
        }

        int needed = plugin.settings().getInt(arena, "gold.bow-at", 10);
        int total = participant.addGold(1);
        plugin.stats().addGold(participant.uuid(), 1);
        plugin.sounds().play(player, "gold-pickup");
        if (total < needed) {
            player.sendActionBar(plugin.msg().get(player, "gold-collected",
                    Ph.of("amount", total, "needed", needed)));
        } else {
            player.sendActionBar(plugin.msg().get(player, "gold-full", Ph.of("amount", total)));
        }
        if (total >= needed) {
            grantBow(participant, player);
        }
        return true;
    }

    private void grantBow(GamePlayer participant, Player player) {
        if (participant.hasBow()) {
            return;
        }
        if (participant.role() == Role.MURDERER
                && !plugin.settings().getBoolean(arena, "gold.murderer-gets-bow", false)) {
            return;
        }
        arena.giveBow(participant, true);
        if (plugin.settings().getBoolean(arena, "gold.broadcast-bow", false)) {
            arena.broadcast("bow-broadcast", Ph.of("player", player.getName()));
        }
    }

    public void clear() {
        for (Item item : active.values()) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        active.clear();
        respawn.clear();
    }

    public int activeCount() {
        return active.size();
    }
}
