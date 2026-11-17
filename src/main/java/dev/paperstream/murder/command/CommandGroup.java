package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandGroup {

    protected final MurderPlugin plugin;

    protected CommandGroup(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract List<SubCommand> build();

    protected Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.msg().send(sender, "player-only");
        throw CommandAbort.silent();
    }

    protected Arena arena(CommandSender sender, String name) {
        Arena arena = plugin.arenas().get(name);
        if (arena == null) {
            plugin.msg().send(sender, "arena-not-found", Ph.of("arena", name));
            throw CommandAbort.silent();
        }
        return arena;
    }

    protected Arena arenaOrCurrent(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return arena(sender, args[index]);
        }
        if (sender instanceof Player player) {
            Arena current = plugin.arenas().arenaOf(player);
            if (current != null) {
                return current;
            }
        }
        throw CommandAbort.usage();
    }

    protected void need(String[] args, int amount) {
        if (args.length < amount) {
            throw CommandAbort.usage();
        }
    }

    protected List<String> arenaNames() {
        return plugin.arenas().names();
    }

    protected List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return names;
    }

    protected void audit(CommandSender sender, String action, String detail) {
        plugin.audit().write(sender, action, detail);
    }
}
