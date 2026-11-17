package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.util.Ph;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MurderCommand implements CommandExecutor, TabCompleter {

    private final MurderPlugin plugin;
    private final Map<String, SubCommand> subs = new LinkedHashMap<>();

    public MurderCommand(MurderPlugin plugin) {
        this.plugin = plugin;
        register(new PlayerCommands(plugin).build());
        register(new AdminCommands(plugin).build());
        register(new ArenaCommands(plugin).build());
        register(new PointCommands(plugin).build());
        register(new ZoneCommands(plugin).build());
        register(new TrapCommands(plugin).build());
        register(new NpcCommands(plugin).build());
        register(new KaliCommands(plugin).build());
        register(new ReplayCommands(plugin).build());
    }

    private void register(List<SubCommand> list) {
        for (SubCommand sub : list) {
            subs.put(sub.name().toLowerCase(Locale.ROOT), sub);
        }
    }

    public Map<String, SubCommand> subs() {
        return subs;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            String topic = args.length > 1 ? args[1] : null;
            int page = args.length > 2 ? parsePage(args[2]) : 1;
            if (topic == null) {
                plugin.help().sendOverview(sender);
            } else {
                plugin.help().sendTopic(sender, topic, page);
            }
            return true;
        }
        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            plugin.msg().send(sender, "unknown-command");
            return true;
        }
        if (!sub.permission().isEmpty() && !sender.hasPermission(sub.permission())) {
            plugin.msg().send(sender, "no-permission");
            return true;
        }
        if (sub.playerOnly() && !(sender instanceof Player)) {
            plugin.msg().send(sender, "player-only");
            return true;
        }
        try {
            sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (CommandAbort abort) {

            if (abort.showUsage()) {
                plugin.help().sendUsage(sender, sub);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("/murder " + sub.name() + " failed: " + exception);
            plugin.msg().send(sender, "unknown-command");
        }
        return true;
    }

    private int parsePage(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        if (args.length <= 1) {
            List<String> names = new ArrayList<>();
            names.add("help");
            for (SubCommand sub : subs.values()) {
                if (sub.permission().isEmpty() || sender.hasPermission(sub.permission())) {
                    names.add(sub.name());
                }
            }
            return filter(names, args.length == 0 ? "" : args[0]);
        }
        if ("help".equalsIgnoreCase(args[0])) {
            return filter(plugin.help().topics(sender), args[1]);
        }
        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || (!sub.permission().isEmpty() && !sender.hasPermission(sub.permission()))) {
            return List.of();
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return filter(sub.complete(sender, rest), rest[rest.length - 1]);
    }

    public static List<String> filter(List<String> options, String prefix) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }

    public static int number(MurderPlugin plugin, CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            plugin.msg().send(sender, "number-invalid", Ph.of("input", raw));
            throw CommandAbort.silent();
        }
    }
}
