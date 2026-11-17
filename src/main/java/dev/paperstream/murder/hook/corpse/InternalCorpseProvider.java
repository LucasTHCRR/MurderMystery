package dev.paperstream.murder.hook.corpse;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.hook.CorpseProvider;
import dev.paperstream.murder.util.Tags;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class InternalCorpseProvider implements CorpseProvider {

    // Mannequin gibt es erst ab 1.21.9
    private static final boolean MANNEQUIN = hasMannequin();

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
        Entity body = MANNEQUIN ? MannequinBody.spawn(spot, victim, skin) : headBody(spot, victim, skin);
        Tags.tag(body, "", Tags.KIND_CORPSE);
        spawned.add(body);
        if (seconds > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(body), seconds * 20L);
        }
        return body;
    }

    private static boolean hasMannequin() {
        try {
            Class.forName("org.bukkit.entity.Mannequin");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Entity headBody(Location spot, Player victim, boolean skin) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (skin && head.getItemMeta() instanceof SkullMeta meta) {
            meta.setPlayerProfile(victim.getPlayerProfile());
            head.setItemMeta(meta);
        }
        return spot.getWorld().spawn(spot.clone().add(0, -1.4, 0), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.getEquipment().setHelmet(head);
        });
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
