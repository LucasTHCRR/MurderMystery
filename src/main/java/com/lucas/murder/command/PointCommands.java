package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.util.Locations;
import com.lucas.murder.util.Ph;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PointCommands extends CommandGroup {

    public PointCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("setlobby")
                .permission("murder.admin")
                .usage("/murder setlobby [arena]")
                .topic("setup")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    Player player = player(sender);
                    Location location = player.getLocation();
                    if (args.length > 0) {
                        Arena arena = arena(sender, args[0]);
                        arena.lobby(location);
                        plugin.arenas().save();
                        plugin.msg().send(sender, "lobby-set-arena", Ph.of("arena", arena.name()));
                        return;
                    }
                    plugin.arenas().globalLobby(location);
                    plugin.msg().send(sender, "lobby-set", Ph.of(
                            "world", location.getWorld().getName(),
                            "x", location.getBlockX(), "y", location.getBlockY(), "z", location.getBlockZ()));
                }));

        list.add(SimpleSub.of("setexit")
                .permission("murder.admin")
                .usage("/murder setexit")
                .topic("setup")
                .run((sender, args) -> {
                    plugin.arenas().exit(player(sender).getLocation());
                    plugin.msg().send(sender, "exit-set");
                }));

        list.add(SimpleSub.of("setspawn")
                .permission("murder.admin")
                .usage("/murder setspawn <arena>")
                .topic("spawns")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    arena.spawns().add(player(sender).getLocation());
                    plugin.arenas().save();
                    plugin.msg().send(sender, "spawn-added", Ph.of(
                            "index", arena.spawns().size(),
                            "arena", arena.name(),
                            "total", arena.spawns().size()));
                }));

        list.add(SimpleSub.of("delspawn")
                .permission("murder.admin")
                .usage("/murder delspawn <arena> <nr>")
                .topic("spawns")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 2);
                    Arena arena = arena(sender, args[0]);
                    int index = MurderCommand.number(plugin, sender, args[1]);
                    if (index < 1 || index > arena.spawns().size()) {
                        plugin.msg().send(sender, "spawn-list-empty", Ph.of("arena", arena.name()));
                        return;
                    }
                    arena.spawns().remove(index - 1);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "spawn-removed", Ph.of("index", index));
                }));

        list.add(SimpleSub.of("spawns")
                .permission("murder.admin")
                .usage("/murder spawns <arena>")
                .topic("spawns")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    listLocations(sender, arena.spawns(), arena, "spawn-list-header", "spawn-list-empty");
                }));

        list.add(SimpleSub.of("setspectator")
                .permission("murder.admin")
                .usage("/murder setspectator <arena>")
                .topic("spawns")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    arena.spectatorSpawn(player(sender).getLocation());
                    plugin.arenas().save();
                    plugin.msg().send(sender, "spectator-spawn-set", Ph.of("arena", arena.name()));
                }));

        list.add(SimpleSub.of("setgold")
                .permission("murder.admin")
                .usage("/murder setgold <arena>")
                .topic("gold")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    arena.goldSpawns().add(Locations.center(player(sender).getLocation()));
                    plugin.arenas().save();
                    plugin.msg().send(sender, "gold-added", Ph.of(
                            "index", arena.goldSpawns().size(), "total", arena.goldSpawns().size()));
                }));

        list.add(SimpleSub.of("delgold")
                .permission("murder.admin")
                .usage("/murder delgold <arena> <nr>")
                .topic("gold")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 2);
                    Arena arena = arena(sender, args[0]);
                    int index = MurderCommand.number(plugin, sender, args[1]);
                    if (index < 1 || index > arena.goldSpawns().size()) {
                        plugin.msg().send(sender, "gold-list-empty", Ph.of("arena", arena.name()));
                        return;
                    }
                    arena.goldSpawns().remove(index - 1);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "gold-removed", Ph.of("index", index));
                }));

        list.add(SimpleSub.of("golds")
                .permission("murder.admin")
                .usage("/murder golds <arena>")
                .topic("gold")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    listLocations(sender, arena.goldSpawns(), arena,
                            "gold-list-header", "gold-list-empty");
                    showParticles(sender, arena.goldSpawns());
                }));

        list.add(SimpleSub.of("setaltar")
                .permission("murder.admin")
                .usage("/murder setaltar <arena>")
                .topic("kali")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    Player player = player(sender);
                    Block block = player.getTargetBlockExact(6);
                    if (block == null) {
                        plugin.msg().send(sender, "kali-no-altar");
                        return;
                    }
                    arena.altar(block.getLocation());
                    plugin.arenas().save();
                    plugin.msg().send(sender, "kali-altar-set",
                            Ph.of("arena", arena.name(), "block", block.getType().name()));
                }));

        list.add(SimpleSub.of("delaltar")
                .permission("murder.admin")
                .usage("/murder delaltar <arena>")
                .topic("kali")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    arena.altar(null);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "altar-removed", Ph.of("arena", arena.name()));
                }));

        list.add(SimpleSub.of("setpotion")
                .permission("murder.admin")
                .usage("/murder setpotion <arena>")
                .topic("potions")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    Player player = player(sender);
                    Block block = player.getTargetBlockExact(6);
                    Location location = block == null ? player.getLocation() : block.getLocation();
                    arena.potionStations().add(location);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "potion-station-added", Ph.of(
                            "index", arena.potionStations().size(),
                            "total", arena.potionStations().size()));
                }));

        list.add(SimpleSub.of("delpotion")
                .permission("murder.admin")
                .usage("/murder delpotion <arena> <nr>")
                .topic("potions")
                .console()
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 2);
                    Arena arena = arena(sender, args[0]);
                    int index = MurderCommand.number(plugin, sender, args[1]);
                    if (index < 1 || index > arena.potionStations().size()) {
                        plugin.msg().send(sender, "potion-station-list-empty",
                                Ph.of("arena", arena.name()));
                        return;
                    }
                    arena.potionStations().remove(index - 1);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "potion-station-removed", Ph.of("index", index));
                }));

        list.add(SimpleSub.of("potions")
                .permission("murder.admin")
                .usage("/murder potions <arena>")
                .topic("potions")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    listLocations(sender, arena.potionStations(), arena,
                            "spawn-list-header", "potion-station-list-empty");
                    showParticles(sender, arena.potionStations());
                }));

        list.add(SimpleSub.of("setsign")
                .permission("murder.admin")
                .usage("/murder setsign <arena>")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    Player player = player(sender);
                    Block block = player.getTargetBlockExact(6);
                    if (block == null || !(block.getState() instanceof Sign)) {
                        throw CommandAbort.usage();
                    }
                    plugin.signs().add(block.getLocation(), arena);
                    plugin.msg().send(sender, "sign-created", Ph.of("arena", arena.name()));
                }));

        return list;
    }

    private void listLocations(CommandSender sender, List<Location> locations, Arena arena,
                               String headerKey, String emptyKey) {
        if (locations.isEmpty()) {
            plugin.msg().send(sender, emptyKey, Ph.of("arena", arena.name()));
            return;
        }
        plugin.msg().send(sender, headerKey, Ph.of("arena", arena.name()));
        for (int i = 0; i < locations.size(); i++) {
            Location location = locations.get(i);
            plugin.msg().send(sender, "spawn-list-entry", Ph.of(
                    "index", i + 1,
                    "world", location.getWorld() == null ? "?" : location.getWorld().getName(),
                    "x", location.getBlockX(), "y", location.getBlockY(), "z", location.getBlockZ()));
        }
    }

    private void showParticles(CommandSender sender, List<Location> locations) {
        if (!(sender instanceof Player player)) {
            return;
        }
        for (Location location : locations) {
            if (location.getWorld() != null && location.getWorld().equals(player.getWorld())) {
                player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                        location.clone().add(0, 1, 0), 12, 0.2, 0.4, 0.2, 0);
            }
        }
    }
}
