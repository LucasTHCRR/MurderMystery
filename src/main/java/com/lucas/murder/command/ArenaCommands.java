package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.util.Locations;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Registries;
import com.lucas.murder.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArenaCommands extends CommandGroup {

    private static final List<String> OPTIONS = List.of(
            "minplayers", "maxplayers", "time", "countdown", "displayname",
            "author", "icon", "goldrespawn", "murderers", "detectives");

    public ArenaCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("create")
                .permission("murder.admin")
                .usage("/murder create <arena>")
                .topic("arena")
                .console()
                .run((sender, args) -> {
                    need(args, 1);
                    if (plugin.arenas().get(args[0]) != null) {
                        plugin.msg().send(sender, "arena-exists", Ph.of("arena", args[0]));
                        return;
                    }
                    plugin.arenas().create(args[0]);
                    plugin.msg().send(sender, "arena-created", Ph.of("arena", args[0]));
                    audit(sender, "create", args[0]);
                }));

        list.add(SimpleSub.of("delete")
                .permission("murder.admin")
                .usage("/murder delete <arena> confirm")
                .topic("arena")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of("confirm"))
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                        plugin.msg().send(sender, "confirm-needed",
                                Ph.of("command", "/murder delete " + arena.name() + " confirm"));
                        return;
                    }
                    plugin.arenas().delete(arena);
                    plugin.msg().send(sender, "arena-deleted", Ph.of("arena", arena.name()));
                    audit(sender, "delete", arena.name());
                }));

        list.add(SimpleSub.of("clone")
                .permission("murder.admin")
                .usage("/murder clone <from> <to>")
                .topic("arena")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 2);
                    Arena source = arena(sender, args[0]);
                    if (plugin.arenas().get(args[1]) != null) {
                        plugin.msg().send(sender, "arena-exists", Ph.of("arena", args[1]));
                        return;
                    }
                    plugin.arenas().clone(source, args[1]);
                    plugin.msg().send(sender, "arena-cloned", Ph.of("from", args[0], "to", args[1]));
                }));

        list.add(SimpleSub.of("rename")
                .permission("murder.admin")
                .usage("/murder rename <old> <new>")
                .topic("arena")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 2);
                    Arena source = arena(sender, args[0]);
                    if (plugin.arenas().get(args[1]) != null) {
                        plugin.msg().send(sender, "arena-exists", Ph.of("arena", args[1]));
                        return;
                    }
                    plugin.arenas().rename(source, args[1]);
                    plugin.msg().send(sender, "arena-renamed", Ph.of("from", args[0], "to", args[1]));
                }));

        list.add(SimpleSub.of("enable")
                .permission("murder.admin")
                .usage("/murder enable <arena>")
                .topic("arena")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    if (!arena.ready()) {
                        plugin.msg().send(sender, "arena-incomplete",
                                Ph.of("missing", String.join(", ", arena.missingSetup())));
                        return;
                    }
                    arena.enabled(true);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "arena-enabled", Ph.of("arena", arena.name()));
                    audit(sender, "enable", arena.name());
                }));

        list.add(SimpleSub.of("disable")
                .permission("murder.admin")
                .usage("/murder disable <arena>")
                .topic("arena")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    for (Player player : new ArrayList<>(arena.onlinePlayers())) {
                        arena.leave(player, false);
                    }
                    arena.enabled(false);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "arena-disabled", Ph.of("arena", arena.name()));
                    audit(sender, "disable", arena.name());
                }));

        list.add(SimpleSub.of("info")
                .permission("murder.admin")
                .usage("/murder info <arena>")
                .topic("setup")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    plugin.msg().send(sender, "info-header", Ph.of("arena", arena.name()));
                    line(sender, "Lobby", arena.waitingLobby() == null ? null
                            : Locations.pretty(arena.waitingLobby()));
                    line(sender, "Exit", plugin.arenas().exit() == null ? null
                            : Locations.pretty(plugin.arenas().exit()));
                    line(sender, "Bounds", arena.bounds() == null ? null
                            : arena.bounds().size() + " blocks");
                    line(sender, "Spawns", arena.spawns().isEmpty() ? null
                            : arena.spawns().size() + "/" + arena.maxPlayers());
                    line(sender, "Spectator", arena.spectatorSpawn() == null ? null
                            : Locations.pretty(arena.spectatorSpawn()));
                    line(sender, "Gold spawns", arena.goldSpawns().isEmpty() ? null
                            : String.valueOf(arena.goldSpawns().size()));
                    line(sender, "Zones", arena.zones().isEmpty() ? null
                            : String.valueOf(arena.zones().size()));
                    line(sender, "Altar", arena.altar() == null ? null : Locations.pretty(arena.altar()));
                    line(sender, "Brewing stations", arena.potionStations().isEmpty() ? null
                            : String.valueOf(arena.potionStations().size()));
                    sender.sendMessage(Text.of(arena.ready()
                            ? Text.fill(plugin.msg().rawString("info-ready"),
                            Ph.of("arena", arena.name()))
                            : plugin.msg().rawString("info-not-ready")));
                }));

        list.add(SimpleSub.of("tp")
                .permission("murder.admin")
                .usage("/murder tp <arena> [spawn|lobby|gold <nr>]")
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> arenaNames();
                    case 2 -> List.of("spawn", "lobby", "spectator", "gold", "altar");
                    default -> List.of();
                })
                .run(this::teleport));

        list.add(SimpleSub.of("set")
                .permission("murder.admin")
                .usage("/murder set <arena> <option> <value>")
                .topic("arena")
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> arenaNames();
                    case 2 -> OPTIONS;
                    case 3 -> List.of("4", "8", "12", "16");
                    default -> List.of();
                })
                .run(this::set));

        list.add(SimpleSub.of("setup")
                .permission("murder.admin")
                .usage("/murder setup <start|here|next|back|goto|status|cancel|finish> [value]")
                .topic("setup")
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> List.of("start", "here", "next", "back", "goto", "status", "cancel", "finish");
                    case 2 -> "start".equalsIgnoreCase(args[0]) ? arenaNames() : List.of();
                    default -> List.of();
                })
                .run((sender, args) -> plugin.wizard().handle(player(sender), args)));

        return list;
    }

    private void line(CommandSender sender, String name, String value) {
        if (value == null) {
            sender.sendMessage(Text.of(Text.fill(plugin.msg().rawString("info-line-missing"),
                    Ph.of("name", name))));
        } else {
            sender.sendMessage(Text.of(Text.fill(plugin.msg().rawString("info-line-ok"),
                    Ph.of("name", name, "value", value))));
        }
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = player(sender);
        need(args, 1);
        Arena arena = arena(sender, args[0]);
        String what = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        Location target = switch (what) {
            case "lobby" -> arena.waitingLobby();
            case "spectator" -> arena.spectatorSpawn();
            case "altar" -> arena.altar();
            case "gold" -> {
                int index = args.length > 2 ? MurderCommand.number(plugin, sender, args[2]) : 1;
                yield index >= 1 && index <= arena.goldSpawns().size()
                        ? arena.goldSpawns().get(index - 1) : null;
            }
            default -> {
                int index = args.length > 2 ? MurderCommand.number(plugin, sender, args[2]) : 1;
                yield index >= 1 && index <= arena.spawns().size()
                        ? arena.spawns().get(index - 1) : null;
            }
        };
        if (target == null) {
            plugin.msg().send(sender, "spawn-list-empty", Ph.of("arena", arena.name()));
            return;
        }
        player.teleport(target);
        plugin.msg().send(sender, "teleported", Ph.of("target", what));
    }

    private void set(CommandSender sender, String[] args) {
        need(args, 2);
        Arena arena = arena(sender, args[0]);
        String option = args[1].toLowerCase(Locale.ROOT);
        String value = args.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";

        switch (option) {
            case "minplayers" -> arena.minPlayers(MurderCommand.number(plugin, sender, value));
            case "maxplayers" -> arena.maxPlayers(MurderCommand.number(plugin, sender, value));
            case "time" -> arena.roundTime(MurderCommand.number(plugin, sender, value));
            case "countdown" -> arena.countdownTime(MurderCommand.number(plugin, sender, value));
            case "displayname" -> arena.displayName(value);
            case "author" -> arena.author(value);
            case "murderers" -> arena.forcedMurderers(MurderCommand.number(plugin, sender, value));
            case "detectives" -> arena.forcedDetectives(MurderCommand.number(plugin, sender, value));
            case "goldrespawn" -> {
                var overrides = plugin.configs().arenas()
                        .createSection("arenas." + arena.name() + ".overrides");
                overrides.set("gold.respawn-seconds", MurderCommand.number(plugin, sender, value));
                arena.overrides(overrides);
            }
            case "icon" -> {
                Player player = player(sender);
                Material material = player.getInventory().getItemInMainHand().getType();
                if (material.isAir()) {
                    material = Registries.material(value);
                }
                if (material == null) {
                    plugin.msg().send(sender, "setting-unknown", Ph.of("option", option));
                    return;
                }
                arena.icon(material);
                plugin.msg().send(sender, "icon-set", Ph.of("item", material.name()));
                plugin.arenas().save();
                return;
            }
            default -> {
                plugin.msg().send(sender, "setting-unknown", Ph.of("option", option));
                return;
            }
        }
        plugin.arenas().save();
        plugin.msg().send(sender, "setting-changed", Ph.of("option", option, "value", value));
        audit(sender, "set", arena.name() + " " + option + " = " + value);
    }
}
