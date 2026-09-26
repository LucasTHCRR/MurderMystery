package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Tags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class WandListener implements Listener {

    private final MurderPlugin plugin;

    public WandListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || !Tags.has(event.getItem(), Tags.WAND)) {
            return;
        }
        if (!player.hasPermission("murder.admin") || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        var location = event.getClickedBlock().getLocation();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.selections().setFirst(player.getUniqueId(), location);
            plugin.msg().send(player, "pos1-set", Ph.of(
                    "x", location.getBlockX(), "y", location.getBlockY(), "z", location.getBlockZ()));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.selections().setSecond(player.getUniqueId(), location);
            plugin.msg().send(player, "pos2-set", Ph.of(
                    "x", location.getBlockX(), "y", location.getBlockY(), "z", location.getBlockZ()));
        }
    }
}
