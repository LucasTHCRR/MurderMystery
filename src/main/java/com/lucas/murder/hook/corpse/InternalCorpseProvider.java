package com.lucas.murder.hook.corpse;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.hook.CorpseProvider;
import com.lucas.murder.util.Tags;
import org.bukkit.Location;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

import java.util.ArrayList;
import java.util.List;

public final class InternalCorpseProvider implements CorpseProvider {

    private final MurderPlugin plugin;
    private final List<Entity> spawned = new ArrayList<>();

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
        boolean skin = plugin.settings().getBoolean(null, "corpses.use-victim-skin", true);
        Location spot = location.clone();
        spot.setPitch(0F);
        Mannequin body = location.getWorld().spawn(spot, Mannequin.class, entity -> {
            if (skin) {
                entity.setProfile(ResolvableProfile.resolvableProfile(victim.getPlayerProfile()));
            }
            entity.setPose(Mannequin.validPoses().contains(Pose.SLEEPING) ? Pose.SLEEPING : Pose.SWIMMING, true);
            entity.setImmovable(true);
            entity.setInvulnerable(true);
            entity.setAI(false);
            entity.setSilent(true);
            entity.setCollidable(false);
            entity.setCustomNameVisible(false);
            entity.setDescription(null);
            entity.setPersistent(false);
        });
        Tags.tag(body, "", Tags.KIND_CORPSE);
        spawned.add(body);
        if (seconds > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(body), seconds * 20L);
        }
        return body;
    }

    @Override
    public void remove(Object handle) {
        if (handle instanceof Entity body) {
            body.remove();
            spawned.remove(body);
        }
    }

    @Override
    public void removeAll() {
        for (Entity body : new ArrayList<>(spawned)) {
            body.remove();
        }
        spawned.clear();
    }

    @Override
    public boolean supports(Feature feature) {
        return feature == Feature.HOLOGRAM;
    }
}
