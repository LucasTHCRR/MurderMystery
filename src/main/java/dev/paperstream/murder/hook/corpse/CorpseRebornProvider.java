package dev.paperstream.murder.hook.corpse;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.hook.CorpseProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class CorpseRebornProvider implements CorpseProvider {

    private final MurderPlugin plugin;
    private final List<Object> spawned = new ArrayList<>();
    private Class<?> apiClass;

    public CorpseRebornProvider(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "CorpseReborn";
    }

    @Override
    public boolean isAvailable() {
        if (Bukkit.getPluginManager().getPlugin("CorpseReborn") == null) {
            return false;
        }
        try {
            apiClass = Class.forName("org.golde.bukkit.corpsereborn.nms.CorpseAPI.CorpseAPI");
            return true;
        } catch (Throwable first) {
            try {
                apiClass = Class.forName("org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI");
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    @Override
    public Object spawn(Player victim, Location location, int seconds) {
        if (apiClass == null && !isAvailable()) {
            return null;
        }
        try {
            Method spawn = apiClass.getMethod("spawnCorpse", Player.class, Location.class, ItemStack[].class);
            Object corpse = spawn.invoke(null, victim, location, new ItemStack[0]);
            if (corpse != null) {
                spawned.add(corpse);
                if (seconds > 0) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(corpse), seconds * 20L);
                }
            }
            return corpse;
        } catch (Throwable throwable) {
            plugin.getLogger().fine("CorpseReborn corpse failed: " + throwable.getMessage());
            return null;
        }
    }

    @Override
    public void remove(Object handle) {
        if (handle == null) {
            return;
        }
        try {
            handle.getClass().getMethod("destroy").invoke(handle);
        } catch (Throwable ignored) {

        }
        spawned.remove(handle);
    }

    @Override
    public void removeAll() {
        for (Object corpse : new ArrayList<>(spawned)) {
            remove(corpse);
        }
        spawned.clear();
    }

    @Override
    public boolean supports(Feature feature) {
        return feature != Feature.LOOT;
    }
}
