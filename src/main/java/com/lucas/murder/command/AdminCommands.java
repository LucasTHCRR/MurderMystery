package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GameState;
import com.lucas.murder.game.Role;
import com.lucas.murder.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class AdminCommands extends CommandGroup {

    public AdminCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("start")
                .permission("murder.admin")
                .usage("/murder start [arena]")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    Arena arena = arenaOrCurrent(sender, args, 0);
                    if (arena.state() == GameState.RUNNING || arena.state() == GameState.ENDING) {
                        plugin.msg().send(sender, "arena-running");
                        return;
                    }
                    if (!arena.ready()) {
                        plugin.msg().send(sender, "arena-incomplete",
                                Ph.of("missing", String.join(", ", arena.missingSetup())));
                        return;
                    }
                    arena.broadcast("countdown-forced");
                    arena.start();
                    audit(sender, "start", arena.name());
                }));

        list.add(SimpleSub.of("stop")
                .permission("murder.admin")
                .usage("/murder stop [arena]")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    Arena arena = arenaOrCurrent(sender, args, 0);
                    for (Player player : arena.onlinePlayers()) {
                        plugin.msg().send(player, "game-stopped");
                    }
                    for (Player player : new ArrayList<>(arena.onlinePlayers())) {
                        arena.leave(player, true);
                    }
                    arena.stop(false);
                    audit(sender, "stop", arena.name());
                }));

        list.add(SimpleSub.of("kick")
                .permission("murder.admin")
                .usage("/murder kick <player>")
                .console()
                .tab((sender, args) -> args.length == 1 ? playerNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Player target = Bukkit.getPlayerExact(args[0]);
                    if (target == null) {
                        plugin.msg().send(sender, "player-not-found", Ph.of("player", args[0]));
                        return;
                    }
                    Arena arena = plugin.arenas().arenaOf(target);
                    if (arena == null) {
                        plugin.msg().send(sender, "not-in-game");
                        return;
                    }
                    arena.leave(target, false);
                    plugin.msg().send(target, "kicked");
                    plugin.msg().send(sender, "kick-success", Ph.of("player", target.getName()));
                    audit(sender, "kick", target.getName());
                }));

        list.add(SimpleSub.of("forcerole")
                .permission("murder.admin")
                .usage("/murder forcerole <player> <role>")
                .console()
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> playerNames();
                    case 2 -> List.of("INNOCENT", "DETECTIVE", "MURDERER");
                    default -> List.of();
                })
                .run((sender, args) -> {
                    need(args, 2);
                    Player target = Bukkit.getPlayerExact(args[0]);
                    if (target == null) {
                        plugin.msg().send(sender, "player-not-found", Ph.of("player", args[0]));
                        return;
                    }
                    Role role = Role.parse(args[1]);
                    if (role == null) {
                        plugin.msg().send(sender, "forcerole-unknown", Ph.of("roles", Role.names()));
                        return;
                    }
                    plugin.roles().force(target.getUniqueId(), role);
                    plugin.msg().send(sender, "forcerole-set",
                            Ph.of("player", target.getName(), "role", role.name()));
                    audit(sender, "forcerole", target.getName() + " -> " + role.name());
                }));

        list.add(SimpleSub.of("forcemap")
                .permission("murder.admin")
                .usage("/murder forcemap <arena>")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    plugin.votes().force(arena.name());
                    plugin.msg().send(sender, "vote-forced",
                            Ph.of("map", com.lucas.murder.util.Text.plain(arena.displayName())));
                    audit(sender, "forcemap", arena.name());
                }));

        list.add(SimpleSub.of("reload")
                .permission("murder.admin")
                .usage("/murder reload [file]")
                .topic("config")
                .console()
                .tab((sender, args) -> args.length == 1
                        ? new ArrayList<>(plugin.configs().fileNames()) : List.of())
                .run((sender, args) -> {
                    long start = System.currentTimeMillis();
                    for (Arena arena : plugin.arenas().all()) {
                        if (arena.state() == GameState.RUNNING) {
                            plugin.msg().send(sender, "reload-warning");
                            break;
                        }
                    }
                    if (args.length > 0) {
                        if (!plugin.configs().reload(args[0])) {
                            plugin.msg().send(sender, "reload-file-unknown",
                                    Ph.of("files", String.join(", ", plugin.configs().fileNames())));
                            return;
                        }
                        plugin.msg().send(sender, "reload-file", Ph.of("file", args[0]));
                        return;
                    }
                    String error = plugin.reloadAll();
                    if (error != null) {
                        plugin.msg().send(sender, "reload-error", Ph.of("file", error, "error", ""));
                        return;
                    }
                    plugin.msg().send(sender, "reload-success",
                            Ph.of("ms", System.currentTimeMillis() - start));
                    audit(sender, "reload", args.length > 0 ? args[0] : "all");
                }));

        list.add(SimpleSub.of("config")
                .permission("murder.admin")
                .usage("/murder config <get|set|list> <path> [value]")
                .topic("config")
                .console()
                .tab((sender, args) -> args.length == 1 ? List.of("get", "set", "list") : List.of())
                .run(this::config));

        list.add(SimpleSub.of("hooks")
                .permission("murder.admin")
                .usage("/murder hooks")
                .console()
                .run((sender, args) -> {
                    plugin.msg().send(sender, "hooks-header");
                    plugin.hooks().detected().forEach((name, installed) -> {
                        String status = plugin.msg().rawString(installed
                                ? (plugin.hooks().isEnabled(name) ? "hooks-active" : "hooks-disabled")
                                : "hooks-missing");
                        plugin.msg().send(sender, "hooks-entry", Ph.of("name", name, "status", status));
                    });
                    plugin.msg().send(sender, "hooks-npc-provider",
                            Ph.of("provider", plugin.npcProviders().active().name()));
                    plugin.msg().send(sender, "hooks-corpse-provider",
                            Ph.of("provider", plugin.corpses().active().name()));
                }));

        list.add(SimpleSub.of("cleanup")
                .permission("murder.admin")
                .usage("/murder cleanup [arena]")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    List<Arena> targets = new ArrayList<>();
                    if (args.length > 0) {
                        targets.add(arena(sender, args[0]));
                    } else {
                        targets.addAll(plugin.arenas().all());
                    }
                    for (Arena arena : targets) {
                        long start = System.currentTimeMillis();
                        plugin.msg().send(sender, "cleanup-started", Ph.of("arena", arena.name()));
                        int before = plugin.cleanup().debugEntityCount(arena);
                        arena.reset();
                        plugin.msg().send(sender, "cleanup-done", Ph.of(
                                "arena", arena.name(),
                                "entities", before,
                                "ms", System.currentTimeMillis() - start));
                    }
                    audit(sender, "cleanup", args.length > 0 ? args[0] : "all");
                }));

        list.add(SimpleSub.of("debug")
                .permission("murder.admin")
                .usage("/murder debug")
                .console()
                .run((sender, args) -> {
                    plugin.msg().send(sender, "debug-header");
                    plugin.msg().send(sender, "debug-entry",
                            Ph.of("name", "arenas", "value", plugin.arenas().all().size()));
                    plugin.msg().send(sender, "debug-entry",
                            Ph.of("name", "npc-provider", "value", plugin.npcProviders().active().name()));
                    plugin.msg().send(sender, "debug-entry",
                            Ph.of("name", "npcs", "value", plugin.npcs().all().size()));
                    for (Arena arena : plugin.arenas().all()) {
                        plugin.msg().send(sender, "debug-entry", Ph.of(
                                "name", arena.name(),
                                "value", arena.state() + " " + String.join(" ", plugin.cleanup().report(arena))));
                    }
                }));

        list.add(SimpleSub.of("save")
                .permission("murder.admin")
                .usage("/murder save")
                .console()
                .run((sender, args) -> {
                    plugin.arenas().save();
                    plugin.npcs().save();
                    plugin.stats().save();
                    plugin.signs().save();
                    plugin.msg().send(sender, "saved");
                }));

        return list;
    }

    private void config(CommandSender sender, String[] args) {
        need(args, 2);
        String mode = args[0].toLowerCase(java.util.Locale.ROOT);
        String path = args[1];
        var config = plugin.configs().config();
        switch (mode) {
            case "get" -> {
                if (!config.contains(path)) {
                    plugin.msg().send(sender, "config-not-found", Ph.of("path", path));
                    return;
                }
                plugin.msg().send(sender, "config-get",
                        Ph.of("path", path, "value", String.valueOf(config.get(path))));
            }
            case "set" -> {
                need(args, 3);
                Object old = config.get(path);
                String raw = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                config.set(path, parse(raw));
                plugin.configs().save("config.yml");
                plugin.configs().reload("config.yml");
                plugin.msg().send(sender, "config-set",
                        Ph.of("path", path, "value", raw, "old", String.valueOf(old)));
                audit(sender, "config set", path + " = " + raw);
            }
            case "list" -> {
                var section = config.getConfigurationSection(path);
                if (section == null) {
                    plugin.msg().send(sender, "config-not-found", Ph.of("path", path));
                    return;
                }
                plugin.msg().send(sender, "config-list-header", Ph.of("path", path));
                for (String key : section.getKeys(false)) {
                    plugin.msg().send(sender, "config-list-entry",
                            Ph.of("key", key, "value", String.valueOf(section.get(key))));
                }
            }
            default -> throw CommandAbort.usage();
        }
    }

    private Object parse(String raw) {
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }
}
