package com.lucas.murder.hook.corpse;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.lucas.murder.MurderPlugin;
import com.lucas.murder.hook.CorpseProvider;
import com.lucas.murder.util.Tags;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public final class InternalCorpseProvider implements CorpseProvider {

    private final MurderPlugin plugin;
    private final List<ArmorStand> spawned = new ArrayList<>();

    public InternalCorpseProvider(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "Internal";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Object spawn(Player victim, Location location, int seconds) {
        if (location.getWorld() == null) {
            return null;
        }
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setBasePlate(false);
            entity.setArms(true);
            entity.setMarker(false);
            entity.setCollidable(false);
            entity.setCustomNameVisible(false);
            entity.setPersistent(false);

            entity.setBodyPose(new EulerAngle(Math.toRadians(90), 0, 0));
            entity.setHeadPose(new EulerAngle(Math.toRadians(90), 0, 0));
        });
        if (plugin.settings().getBoolean(null, "corpses.use-victim-skin", true)) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                PlayerProfile profile = victim.getPlayerProfile();
                meta.setPlayerProfile(profile);
                head.setItemMeta(meta);
            }
            EntityEquipment equipment = stand.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(head);
            }
        }
        Tags.tag(stand, "", Tags.KIND_CORPSE);
        spawned.add(stand);
        if (seconds > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(stand), seconds * 20L);
        }
        return stand;
    }

    @Override
    public void remove(Object handle) {
        if (handle instanceof ArmorStand stand) {
            stand.remove();
            spawned.remove(stand);
        }
    }

    @Override
    public void removeAll() {
        for (ArmorStand stand : new ArrayList<>(spawned)) {
            stand.remove();
        }
        spawned.clear();
    }

    @Override
    public boolean supports(Feature feature) {
        return feature == Feature.HOLOGRAM;
    }
}
