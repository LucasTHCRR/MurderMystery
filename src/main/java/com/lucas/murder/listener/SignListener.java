package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SignListener implements Listener {

    private final MurderPlugin plugin;

    public SignListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Arena arena = plugin.signs().arenaAt(event.getClickedBlock().getLocation());
        if (arena == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("murder.play")) {
            plugin.msg().send(player, "no-permission");
            return;
        }
        player.performCommand("murder join " + arena.name());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.signs().arenaAt(event.getBlock().getLocation()) == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("murder.admin")) {
            event.setCancelled(true);
            return;
        }
        plugin.signs().remove(event.getBlock().getLocation());
        plugin.msg().send(event.getPlayer(), "sign-removed");
    }
}
