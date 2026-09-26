package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.GameState;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Tags;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class GameListener implements Listener {

    private final MurderPlugin plugin;

    public GameListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        event.setCancelled(true);
        if (arena.state() != GameState.RUNNING) {
            return;
        }
        Item item = event.getItem();
        String kind = Tags.kindOf(item);
        if (kind == null) {
            return;
        }

        if (plugin.kali().blocksPickup(player.getUniqueId(), item.getItemStack().getType())) {
            return;
        }
        switch (kind) {
            case Tags.KIND_GOLD -> arena.gold().collect(player, item);
            case Tags.KIND_BOW -> arena.pickupBow(player, item);
            case Tags.KIND_KNIFE -> {
                GamePlayer participant = arena.participant(player.getUniqueId());
                if (participant != null && participant.role() == com.lucas.murder.game.Role.MURDERER
                        && participant.alive()) {
                    item.remove();
                    player.getInventory().setItem(plugin.items().slot("knife", 0),
                            plugin.items().knife(plugin.stats().equippedMaterial(player.getUniqueId(), "knife")));
                }
            }
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Arena arena = plugin.arenas().arenaOf(event.getPlayer());
        if (arena == null) {
            return;
        }
        if (Tags.has(event.getItemDrop().getItemStack(), Tags.POTION_COLOR)
                && plugin.potions().dropAllowed()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.arenas().arenaOf(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.arenas().arenaOf(player) != null
                && !(event.getInventory().getHolder() instanceof com.lucas.murder.gui.Menu)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.arenas().arenaOf(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.arenas().arenaOf(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        ItemStack hand = event.getItem();

        if (arena.state() != GameState.RUNNING && hand != null && isRightClick(event.getAction())) {
            Material type = hand.getType();
            if (type == plugin.items().build("lobby.leave", Material.RED_BED).getType()) {
                event.setCancelled(true);
                arena.leave(player, false);
                return;
            }
            if (type == plugin.items().build("lobby.vote", Material.PAPER).getType()) {
                event.setCancelled(true);
                plugin.menus().openVote(player);
                return;
            }
            if (type == plugin.items().build("lobby.shop", Material.CHEST).getType()) {
                event.setCancelled(true);
                plugin.menus().openShop(player);
                return;
            }
        }

        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant != null && (participant.spectator() || !participant.alive())) {
            if (hand != null && hand.getType() == Material.COMPASS && isRightClick(event.getAction())) {
                event.setCancelled(true);
                teleportToRandomPlayer(arena, player);
            }
            if (hand != null && hand.getType() == Material.RED_BED && isRightClick(event.getAction())) {
                event.setCancelled(true);
                arena.leave(player, false);
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        Location location = block.getLocation();

        if (arena.altar() != null && sameBlock(arena.altar(), location) && plugin.kali().enabled()) {
            event.setCancelled(true);
            plugin.kali().click(arena, player, hand);
            return;
        }

        for (Location station : arena.potionStations()) {
            if (sameBlock(station, location)) {
                event.setCancelled(true);
                plugin.potions().buy(arena, player);
                return;
            }
        }
    }

    private boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private void teleportToRandomPlayer(Arena arena, Player player) {
        List<GamePlayer> alive = new java.util.ArrayList<>();
        for (GamePlayer participant : arena.participants()) {
            if (participant.alive() && !participant.spectator() && participant.online()) {
                alive.add(participant);
            }
        }
        if (alive.isEmpty()) {
            plugin.msg().send(player, "spectator-nobody");
            return;
        }
        GamePlayer target = alive.get((int) (Math.random() * alive.size()));
        Player targetPlayer = target.player();
        if (targetPlayer != null) {
            player.teleport(targetPlayer.getLocation());
            plugin.msg().send(player, "spectator-teleport", Ph.of("player", targetPlayer.getName()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Arena arena = plugin.arenas().arenaOf(event.getPlayer());
        if (arena == null) {
            return;
        }
        if (Tags.has(event.getItem(), Tags.POTION_COLOR)) {
            plugin.potions().drink(arena, event.getPlayer(), event.getItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.kali().isFrozen(player.getUniqueId())) {
            return;
        }

        // Einfrieren per Positionsvergleich, ein Slowness-Icon würde den Fluch verraten
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to.getX() != from.getX() || to.getZ() != from.getZ() || to.getY() != from.getY()) {
            Location fixed = from.clone();
            fixed.setYaw(to.getYaw());
            fixed.setPitch(to.getPitch());
            event.setTo(fixed);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        GamePlayer participant = arena.participant(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);
            if (arena.spectatorSpawn() != null) {
                player.teleport(arena.spectatorSpawn());
            }
        });
        if (participant != null && participant.alive()) {
            arena.kill(participant, null, "death-generic");
        }
    }
}
