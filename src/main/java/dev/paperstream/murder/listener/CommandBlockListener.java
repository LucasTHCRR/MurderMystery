package dev.paperstream.murder.listener;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandBlockListener implements Listener {

    private final MurderPlugin plugin;

    public CommandBlockListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        ConfigurationSection section = plugin.settings().root()
                .getConfigurationSection("blocked-commands");
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        if (!blockedInState(section, arena.state())) {
            return;
        }
        String bypass = section.getString("bypass-permission", "");
        if (!bypass.isEmpty() && player.hasPermission(bypass)) {
            return;
        }

        Set<String> names = normalise(event.getMessage());
        if (names.isEmpty()) {
            return;
        }
        List<String> always = lower(section.getStringList("always-allowed"));
        for (String name : names) {
            if (always.contains(name)) {
                return;
            }
        }

        boolean whitelist = "WHITELIST".equalsIgnoreCase(section.getString("mode", "BLACKLIST"));
        List<String> list = lower(section.getStringList(whitelist ? "whitelist" : "blacklist"));
        boolean listed = false;
        for (String name : names) {
            if (list.contains(name)) {
                listed = true;
                break;
            }
        }
        boolean blocked = whitelist != listed;
        if (!blocked) {
            return;
        }
        event.setCancelled(true);
        plugin.msg().send(player, whitelist ? "command-blocked-whitelist" : "command-blocked",
                Ph.of("command", "/" + names.iterator().next()));
        plugin.msg().send(player, "command-blocked-hint");
        String sound = section.getString("sound", "");
        if (!sound.isEmpty()) {
            plugin.sounds().playRaw(player, sound, 1F, 0.5F);
        }
    }

    private boolean blockedInState(ConfigurationSection section, GameState state) {
        return switch (state) {
            case WAITING -> section.getBoolean("block-in.lobby", true);
            case COUNTDOWN -> section.getBoolean("block-in.countdown", true);
            case RUNNING -> section.getBoolean("block-in.ingame", true);
            case ENDING -> section.getBoolean("block-in.ending", true);
            default -> false;
        };
    }

    private Set<String> normalise(String message) {
        Set<String> names = new HashSet<>();
        String raw = message.startsWith("/") ? message.substring(1) : message;
        int space = raw.indexOf(' ');
        String label = (space < 0 ? raw : raw.substring(0, space)).toLowerCase(Locale.ROOT);
        if (label.isEmpty()) {
            return names;
        }
        names.add(label);
        int colon = label.indexOf(':');
        if (colon >= 0 && colon + 1 < label.length()) {
            names.add(label.substring(colon + 1));
        }
        for (String name : new HashSet<>(names)) {
            Command command = Bukkit.getCommandMap().getCommand(name);
            if (command != null) {
                names.add(command.getName().toLowerCase(Locale.ROOT));
                if (command instanceof PluginCommand pluginCommand) {
                    names.add(pluginCommand.getName().toLowerCase(Locale.ROOT));
                }
            }
        }
        return names;
    }

    private List<String> lower(List<String> input) {
        return input.stream().map(entry -> entry.toLowerCase(Locale.ROOT)).toList();
    }
}
