package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.npc.MurderNpc;
import com.lucas.murder.npc.NpcType;
import com.lucas.murder.stats.PlayerStats;
import com.lucas.murder.util.Ph;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class NpcListener implements Listener {

    private final MurderPlugin plugin;

    public NpcListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handle(event.getPlayer(), plugin.npcs().byEntity(event.getRightClicked()), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event instanceof PlayerInteractAtEntityEvent) {
            return;
        }
        handle(event.getPlayer(), plugin.npcs().byEntity(event.getRightClicked()), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (plugin.npcs().byEntity(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    private void handle(Player player, MurderNpc npc, org.bukkit.event.Cancellable event) {
        if (npc == null) {
            return;
        }
        event.setCancelled(true);
        long cooldown = plugin.settings().getInt(null, "npcs.click-cooldown-ms", 1000);
        if (!plugin.cooldowns().tryUse(player.getUniqueId(), "npc", cooldown)) {
            return;
        }
        plugin.sounds().play(player, "npc-click");
        Arena arena = npc.arena() == null ? null : plugin.arenas().get(npc.arena());

        switch (npc.type()) {
            case JOIN -> {
                if (arena != null) {
                    player.performCommand("murder join " + arena.name());
                }
            }
            case RANDOM -> player.performCommand("murder join");
            case VOTE -> plugin.menus().openVote(player);
            case SHOP -> plugin.menus().openShop(player);
            case STATS -> {
                PlayerStats stats = plugin.stats().get(player.getUniqueId());
                plugin.menus().openStats(player, stats);
            }
            case LEADERBOARD -> {
                plugin.msg().send(player, "leaderboard-header", Ph.of("stat", npc.stat()));
                var top = plugin.stats().leaderboard().top(npc.stat(), 10);
                if (top.isEmpty()) {
                    plugin.msg().send(player, "leaderboard-empty");
                }
                for (int i = 0; i < top.size(); i++) {
                    plugin.msg().send(player, "leaderboard-entry", Ph.of(
                            "rank", i + 1, "player", top.get(i).name(), "value", top.get(i).value()));
                }
            }
            case SPECTATE -> plugin.menus().openSpectate(player);
            case LEAVE -> {
                Arena current = plugin.arenas().arenaOf(player);
                if (current != null) {
                    current.leave(player, false);
                } else {
                    var exit = plugin.arenas().exit();
                    if (exit != null) {
                        player.teleport(exit);
                    }
                }
            }
            case INFO -> plugin.help().sendTopic(player, "setup", 1);
            case REPLAY -> player.performCommand("murder replay list");
            case KALI -> {
                Arena current = plugin.arenas().arenaOf(player);
                if (current != null) {
                    plugin.kali().click(current, player, player.getInventory().getItemInMainHand());
                }
            }
            case POTION -> {
                Arena current = plugin.arenas().arenaOf(player);
                if (current != null) {
                    plugin.potions().buy(current, player);
                }
            }
            default -> {
            }
        }
    }
}
