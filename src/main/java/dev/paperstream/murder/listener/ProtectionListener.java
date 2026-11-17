package dev.paperstream.murder.listener;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.gui.Menu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.List;
import java.util.Locale;

public final class ProtectionListener implements Listener {

    private final MurderPlugin plugin;

    public ProtectionListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean protectedPlayer(Entity entity) {

        // Absichtlich keine Bypass-Permission, spielende Ops würden die Map plündern
        return entity instanceof Player player && plugin.arenas().arenaOf(player) != null;
    }

    private boolean protectedSource(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return protectedPlayer(shooter);
        }
        return protectedPlayer(entity);
    }

    private boolean allowed(Material material) {
        List<String> patterns = plugin.settings().getStringList(null, "protection.allowed-blocks");
        String name = material.name();
        for (String raw : patterns) {
            String pattern = raw.toUpperCase(Locale.ROOT);
            if (pattern.startsWith("*") ? name.endsWith(pattern.substring(1))
                    : pattern.endsWith("*") ? name.startsWith(pattern.substring(0, pattern.length() - 1))
                    : name.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !protectedPlayer(event.getPlayer())) {
            return;
        }
        if (event.getAction() == Action.PHYSICAL) {

            if (!allowed(block.getType())) {
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !allowed(block.getType())) {

            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            return;
        }
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLectern(PlayerTakeLecternBookEvent event) {
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() != null && protectedSource(event.getIgnitingEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        if (protectedSource(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(EntityInteractEvent event) {
        if (protectedSource(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            return;
        }
        if (plugin.npcs().byEntity(event.getRightClicked()) != null) {
            return;
        }
        if (protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (protectedSource(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() != null && protectedSource(event.getRemover())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() != null && protectedSource(event.getAttacker())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() != null && protectedSource(event.getAttacker())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (protectedPlayer(event.getEntered())) {
            event.setCancelled(true);
        }
    }
}
