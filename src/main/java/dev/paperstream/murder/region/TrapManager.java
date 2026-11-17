package dev.paperstream.murder.region;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.util.Ph;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrapManager implements Listener {

    private static final long BLAME_MILLIS = 8000L;

    private final MurderPlugin plugin;
    private final Map<Trap, Map<Location, BlockData>> removed = new HashMap<>();
    private final Map<Trap, BukkitTask> restoreTasks = new HashMap<>();
    private final Map<Trap, Long> readyAt = new HashMap<>();
    private final Map<UUID, UUID> blameBy = new HashMap<>();
    private final Map<UUID, Long> blameUntil = new HashMap<>();

    public TrapManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    // ignoreCancelled aus, der ProtectionListener blockt Hebel sonst vorher
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        Trap trap = find(arena, block);
        if (trap == null) {
            return;
        }
        event.setCancelled(true);
        if (arena.state() != GameState.RUNNING) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return;
        }
        if (!trap.mode().allows(participant.role())) {
            plugin.msg().send(player, "trap-not-allowed");
            return;
        }
        long now = System.currentTimeMillis();
        Long ready = readyAt.get(trap);
        if (removed.containsKey(trap) || (ready != null && ready > now)) {
            long seconds = ready == null ? trap.duration() : Math.max(1, (ready - now + 999) / 1000);
            plugin.msg().send(player, "trap-cooldown", Ph.of("seconds", seconds));
            return;
        }
        if (participant.gold() < trap.cost()) {
            plugin.msg().send(player, "trap-no-gold", Ph.of("cost", trap.cost()));
            return;
        }
        var trapEvent = new dev.paperstream.murder.api.event.MurderTrapTriggerEvent(arena.name(), trap.name(), player);
        plugin.getServer().getPluginManager().callEvent(trapEvent);
        if (trapEvent.isCancelled()) {
            return;
        }
        participant.gold(participant.gold() - trap.cost());
        arena.gold().syncItem(participant, player);
        trigger(arena, trap, player);
        readyAt.put(trap, now + (trap.duration() + trap.cooldown()) * 1000L);
        player.sendActionBar(plugin.msg().get(player, "trap-triggered", Ph.of("trap", trap.name())));
        plugin.sounds().play(player, "zone-warning");
    }

    private Trap find(Arena arena, Block block) {
        for (Trap trap : arena.traps().values()) {
            if (trap.isTrigger(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
                return trap;
            }
        }
        return null;
    }

    private void trigger(Arena arena, Trap trap, Player trigger) {
        Cuboid area = trap.area();
        World world = plugin.getServer().getWorld(area.world() != null ? area.world() : trigger.getWorld().getName());
        if (world == null) {
            return;
        }
        Map<Location, BlockData> saved = new HashMap<>();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int y = area.minY(); y <= area.maxY(); y++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    saved.put(block.getLocation(), block.getBlockData());
                    block.setType(org.bukkit.Material.AIR, false);
                }
            }
        }
        removed.put(trap, saved);

        long until = System.currentTimeMillis() + trap.duration() * 1000L + BLAME_MILLIS;
        for (GamePlayer other : arena.participants()) {
            Player player = other.player();
            if (player == null || !other.alive() || player.equals(trigger)) {
                continue;
            }
            if (area.contains(player.getLocation(), 2)) {
                blameBy.put(other.uuid(), trigger.getUniqueId());
                blameUntil.put(other.uuid(), until);
            }
        }

        restoreTasks.put(trap, plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> restore(trap), Math.max(1, trap.duration()) * 20L));
    }

    private void restore(Trap trap) {
        BukkitTask task = restoreTasks.remove(trap);
        if (task != null) {
            task.cancel();
        }
        Map<Location, BlockData> saved = removed.remove(trap);
        if (saved == null) {
            return;
        }
        for (Map.Entry<Location, BlockData> entry : saved.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue(), false);
        }
    }

    public Player blame(UUID victim) {
        Long until = blameUntil.remove(victim);
        UUID by = blameBy.remove(victim);
        if (until == null || by == null || until < System.currentTimeMillis()) {
            return null;
        }
        return plugin.getServer().getPlayer(by);
    }

    public void resetArena(Arena arena) {
        for (Trap trap : new ArrayList<>(arena.traps().values())) {
            restore(trap);
            readyAt.remove(trap);
        }
        List<UUID> ids = new ArrayList<>();
        for (GamePlayer participant : arena.participants()) {
            ids.add(participant.uuid());
        }
        ids.forEach(id -> {
            blameBy.remove(id);
            blameUntil.remove(id);
        });
    }

    public void restoreAll() {
        for (Trap trap : new ArrayList<>(removed.keySet())) {
            restore(trap);
        }
        readyAt.clear();
        blameBy.clear();
        blameUntil.clear();
    }
}
