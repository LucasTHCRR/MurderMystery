package com.lucas.murder.cleanup;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.util.PlayerSnapshot;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Tags;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Spider;

import java.util.ArrayList;
import java.util.List;

public final class CleanupService {

    private final MurderPlugin plugin;

    public CleanupService(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void resetArena(Arena arena) {
        long start = System.currentTimeMillis();
        int removed = 0;

        // Zuerst die Tasks stoppen, sonst füllt ein laufender Task alles wieder auf
        run("tasks", () -> {
            arena.gold().clear();
            plugin.zones().clearAll();
        });

        removed += runCounting("entities", () -> removeEntities(arena));
        run("corpses", () -> plugin.corpses().removeAll());

        run("blocks", () -> {
            if (plugin.settings().getBoolean(arena, "cleanup.reset-weather", true)
                    && arena.bounds() != null && arena.bounds().world() != null) {
                World world = plugin.getServer().getWorld(arena.bounds().world());
                if (world != null) {
                    world.setStorm(false);
                    world.setThundering(false);
                }
            }
        });

        run("players", () -> {
            for (GamePlayer participant : new ArrayList<>(arena.participants())) {
                Player player = participant.player();
                if (player != null) {
                    resetPlayer(arena, participant, player);
                }
            }
        });

        run("data", () -> {
            plugin.kali().clearArena(arena);
            plugin.potions().clearArena(arena);
            arena.participants().clear();
        });

        long ms = System.currentTimeMillis() - start;
        if (plugin.settings().getBoolean(arena, "cleanup.verify-after-reset", true)) {
            int left = countLeftovers(arena);
            if (left > 0) {
                plugin.getLogger().warning("After resetting " + arena.name() + " there were still "
                        + left + " entities of ours around.");
            }
        }
        plugin.getLogger().fine("Arena " + arena.name() + " reset in " + ms + "ms (" + removed + " entities)");
    }

    public void resetPlayer(Arena arena, GamePlayer participant, Player player) {
        try {
            plugin.kali().clearPlayer(participant.uuid(), player);
            plugin.chat().clear(participant.uuid());
            plugin.cooldowns().clear(participant.uuid());
            plugin.zones().clear(participant.uuid());
            plugin.nametags().remove(arena, player);
            plugin.nametags().clear(player);
            plugin.boards().clear(player);

            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
                player.showPlayer(plugin, online);
            }
            participant.snapshot().restore(player);
            Location target = arena.state() == com.lucas.murder.game.GameState.DISABLED
                    ? plugin.arenas().exitOrLobby(player)
                    : plugin.arenas().exitOrLobby(player);
            if (target != null) {
                player.teleport(target);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not reset " + player.getName() + ": " + exception.getMessage());
            PlayerSnapshot.wipe(player);
        }
    }

    private int removeEntities(Arena arena) {
        if (arena.bounds() == null || arena.bounds().world() == null) {
            return 0;
        }
        World world = plugin.getServer().getWorld(arena.bounds().world());
        if (world == null) {
            return 0;
        }
        int padding = plugin.settings().getInt(arena, "cleanup.radius-padding", 10);
        boolean onlyTagged = plugin.settings().getBoolean(arena, "cleanup.only-tagged-items", true);
        int removed = 0;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            boolean ours = Tags.isTagged(entity) && arena.name().equals(Tags.arenaOf(entity));

            if (!ours && !arena.bounds().contains(entity.getLocation(), padding)) {
                continue;
            }

            // Nie blind löschen, sonst gehen Deko und fremde NPCs verloren
            boolean remove = switch (entity) {
                case Item item -> ours || (!onlyTagged
                        && plugin.settings().getBoolean(arena, "cleanup.remove-dropped-items", true));
                case AbstractArrow arrow -> plugin.settings().getBoolean(arena, "cleanup.remove-arrows", true);
                case Projectile projectile -> ours;
                case ExperienceOrb orb -> plugin.settings()
                        .getBoolean(arena, "cleanup.remove-experience-orbs", true);
                case Spider spider -> ours;
                case ArmorStand stand -> ours && plugin.settings()
                        .getBoolean(arena, "cleanup.remove-armorstands", true);
                default -> ours;
            };
            if (remove) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    private int countLeftovers(Arena arena) {
        if (arena.bounds() == null || arena.bounds().world() == null) {
            return 0;
        }
        World world = plugin.getServer().getWorld(arena.bounds().world());
        if (world == null) {
            return 0;
        }
        List<String> found = new ArrayList<>();
        for (Entity entity : world.getEntities()) {

            if (entity.isValid() && Tags.isTagged(entity) && arena.name().equals(Tags.arenaOf(entity))) {
                found.add(entity.getType().name() + "/" + Tags.kindOf(entity) + "@"
                        + entity.getLocation().getBlockX() + "," + entity.getLocation().getBlockY()
                        + "," + entity.getLocation().getBlockZ());
            }
        }
        if (!found.isEmpty()) {
            plugin.getLogger().warning("Leftovers in " + arena.name() + ": " + String.join(" ", found));
        }
        return found.size();
    }

    public int cleanupOnStartup() {
        if (!plugin.settings().getBoolean(null, "cleanup.on-startup", true)) {
            return 0;
        }
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (Tags.isTagged(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " leftovers from a previous run.");
        }
        return removed;
    }

    public int debugEntityCount(Arena arena) {
        return countLeftovers(arena);
    }

    public List<String> report(Arena arena) {
        List<String> lines = new ArrayList<>();
        lines.add("entities=" + debugEntityCount(arena));
        lines.add("gold=" + arena.gold().activeCount());
        lines.add("players=" + arena.playerCount());
        lines.add("alive=" + arena.aliveCount());
        return lines;
    }

    private void run(String step, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            plugin.getLogger().warning("Cleanup step " + step + " failed: " + exception.getMessage());
        }
    }

    private int runCounting(String step, java.util.function.IntSupplier action) {
        try {
            return action.getAsInt();
        } catch (Exception exception) {
            plugin.getLogger().warning("Cleanup step " + step + " failed: " + exception.getMessage());
            return 0;
        }
    }

    public void announce(Player sender, Arena arena, long ms, int entities) {
        plugin.msg().send(sender, "cleanup-done",
                Ph.of("arena", arena.name(), "ms", ms, "entities", entities));
    }
}
