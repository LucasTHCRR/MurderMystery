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
        int before = participant.goldCollected();
        int total = participant.addGold(1);
        plugin.stats().addGold(participant.uuid(), participant.goldCollected() - before);
        plugin.sounds().play(player, "gold-pickup");
        if (total < needed) {
            player.sendActionBar(plugin.msg().get(player, "gold-collected",
                    Ph.of("amount", total, "needed", needed)));
        } else {
            player.sendActionBar(plugin.msg().get(player, "gold-full", Ph.of("amount", total)));
        }

        while (needed > 0 && participant.gold() >= needed && canBuyShots(participant)) {
            participant.gold(participant.gold() - needed);
            grantBow(participant, player);
        }
        syncItem(participant, player);
        return true;
    }

    private boolean canBuyShots(GamePlayer participant) {
        if (participant.role() == Role.DETECTIVE) {
            return false;
        }
        return participant.role() != Role.MURDERER
                || plugin.settings().getBoolean(arena, "gold.murderer-gets-bow", true);
    }

    private void grantBow(GamePlayer participant, Player player) {
        if (participant.role() == Role.DETECTIVE) {
            return;
        }
        if (participant.role() == Role.MURDERER
                && !plugin.settings().getBoolean(arena, "gold.murderer-gets-bow", true)) {
            return;
        }
        if (participant.hasBow()) {
            int slot = plugin.items().slot("arrow", 9);
            ItemStack arrows = player.getInventory().getItem(slot);
            if (arrows != null && arrows.getType() == plugin.items().arrow().getType()) {
                arrows.setAmount(Math.min(64, arrows.getAmount() + 1));
            } else {
                player.getInventory().setItem(slot, plugin.items().arrow());
            }
            plugin.msg().send(player, "arrow-bought");
            plugin.sounds().play(player, "arrow-ready");
            return;
        }
        arena.giveBow(participant, true);
        if (plugin.settings().getBoolean(arena, "gold.broadcast-bow", false)) {
            arena.broadcast("bow-broadcast", Ph.of("player", player.getName()));
        }
    }

    public void syncItem(GamePlayer participant, Player player) {
        int slot = plugin.items().slot("gold", 8);
        if (participant.gold() <= 0) {
            player.getInventory().setItem(slot, null);
            return;
        }
        ItemStack stack = plugin.items().build("gold", Material.GOLD_INGOT);
        stack.setAmount(Math.min(stack.getMaxStackSize(), participant.gold()));
        player.getInventory().setItem(slot, stack);
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
