package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.region.Cuboid;
import com.lucas.murder.region.Zone;
import com.lucas.murder.region.ZoneType;
import com.lucas.murder.util.Ph;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ZoneCommands extends CommandGroup {

    public ZoneCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("wand")
                .permission("murder.admin")
                .usage("/murder wand")
                .topic("zones")
                .run((sender, args) -> {
                    Player player = player(sender);
                    player.getInventory().addItem(plugin.items().wand());
                    plugin.msg().send(sender, "wand-given");
                }));

        list.add(SimpleSub.of("pos1")
                .permission("murder.admin")
                .usage("/murder pos1")
                .topic("zones")
                .run((sender, args) -> {
                    Player player = player(sender);
                    plugin.selections().setFirst(player.getUniqueId(), player.getLocation());
                    plugin.msg().send(sender, "pos1-set", Ph.of(
                            "x", player.getLocation().getBlockX(),
                            "y", player.getLocation().getBlockY(),
                            "z", player.getLocation().getBlockZ()));
                }));

        list.add(SimpleSub.of("pos2")
                .permission("murder.admin")
                .usage("/murder pos2")
                .topic("zones")
                .run((sender, args) -> {
                    Player player = player(sender);
                    plugin.selections().setSecond(player.getUniqueId(), player.getLocation());
                    plugin.msg().send(sender, "pos2-set", Ph.of(
                            "x", player.getLocation().getBlockX(),
                            "y", player.getLocation().getBlockY(),
                            "z", player.getLocation().getBlockZ()));
                }));

        list.add(SimpleSub.of("selection")
                .permission("murder.admin")
                .usage("/murder selection")
                .topic("zones")
                .run((sender, args) -> {
                    Cuboid selection = selection(sender);
                    plugin.msg().send(sender, "selection-info", Ph.of(
                            "size", selection.size(),
                            "dx", selection.width(),
                            "dy", selection.height(),
                            "dz", selection.depth()));
                }));

        list.add(SimpleSub.of("setbounds")
                .permission("murder.admin")
                .usage("/murder setbounds <arena>")
                .topic("zones")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    need(args, 1);
                    Arena arena = arena(sender, args[0]);
                    Cuboid selection = selection(sender);
                    arena.bounds(selection);
                    plugin.arenas().save();
                    plugin.msg().send(sender, "bounds-set",
                            Ph.of("arena", arena.name(), "size", selection.size()));
                }));

        list.add(SimpleSub.of("zone")
                .permission("murder.admin")
                .usage("/murder zone <add|remove|list|show|type|msg> <arena> ...")
                .topic("zones")
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> List.of("add", "remove", "list", "show", "type", "msg");
                    case 2 -> arenaNames();
                    case 3 -> zoneNames(args[1]);
                    case 4 -> "add".equalsIgnoreCase(args[0]) || "type".equalsIgnoreCase(args[0])
                            ? typeNames() : List.of();
                    default -> List.of();
                })
                .run(this::zone));

        return list;
    }

    private List<String> zoneNames(String arenaName) {
        Arena arena = plugin.arenas().get(arenaName);
        return arena == null ? List.of() : new ArrayList<>(arena.zones().keySet());
    }

    private List<String> typeNames() {
        List<String> names = new ArrayList<>();
        for (ZoneType type : ZoneType.values()) {
            names.add(type.name());
        }
        return names;
    }

    private Cuboid selection(CommandSender sender) {
        Player player = player(sender);
        if (plugin.selections().mismatchedWorlds(player.getUniqueId())) {
            plugin.msg().send(sender, "selection-other-world");
            throw CommandAbort.silent();
        }
        Cuboid selection = plugin.selections().selection(player.getUniqueId());
        if (selection == null) {
            plugin.msg().send(sender, "selection-missing");
            throw CommandAbort.silent();
        }
        return selection;
    }

    private void zone(CommandSender sender, String[] args) {
        need(args, 2);
        String mode = args[0].toLowerCase(Locale.ROOT);
        Arena arena = arena(sender, args[1]);

        switch (mode) {
            case "add" -> {
                need(args, 4);
                String name = args[2].toLowerCase(Locale.ROOT);
                if (arena.zones().containsKey(name)) {
                    plugin.msg().send(sender, "zone-exists", Ph.of("zone", name));
                    return;
                }
                ZoneType type = ZoneType.parse(args[3]);
                if (type == null) {
                    plugin.msg().send(sender, "zone-type-unknown", Ph.of("types", ZoneType.names()));
                    return;
                }
                Cuboid selection = selection(sender);
                arena.zones().put(name, new Zone(name, type, selection, "death-zone", 2.0D));
                plugin.arenas().save();
                plugin.msg().send(sender, "zone-added",
                        Ph.of("zone", name, "type", type.name(), "arena", arena.name()));
            }
            case "remove" -> {
                need(args, 3);
                String name = args[2].toLowerCase(Locale.ROOT);
                if (arena.zones().remove(name) == null) {
                    plugin.msg().send(sender, "zone-not-found", Ph.of("zone", name));
                    return;
                }
                plugin.arenas().save();
                plugin.msg().send(sender, "zone-removed", Ph.of("zone", name));
            }
            case "list" -> {
                if (arena.zones().isEmpty()) {
                    plugin.msg().send(sender, "zone-list-empty", Ph.of("arena", arena.name()));
                    return;
                }
                plugin.msg().send(sender, "zone-list-header", Ph.of("arena", arena.name()));
                for (Zone zone : arena.zones().values()) {
                    plugin.msg().send(sender, "zone-list-entry", Ph.of(
                            "zone", zone.name(),
                            "type", zone.type().name(),
                            "size", zone.area().size()));
                }
            }
            case "show" -> {
                Player player = player(sender);
                List<Zone> zones = new ArrayList<>();
                if (args.length > 2) {
                    Zone zone = arena.zones().get(args[2].toLowerCase(Locale.ROOT));
                    if (zone == null) {
                        plugin.msg().send(sender, "zone-not-found", Ph.of("zone", args[2]));
                        return;
                    }
                    zones.add(zone);
                } else {
                    zones.addAll(arena.zones().values());
                }
                plugin.msg().send(sender, "zone-showing", Ph.of("seconds", 15));

                for (int second = 0; second < 15; second++) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) {
                            return;
                        }
                        for (Zone zone : zones) {
                            zone.area().outline(player, Particle.HAPPY_VILLAGER, 1.0D);
                        }
                    }, second * 20L);
                }
            }
            case "type" -> {
                need(args, 4);
                Zone zone = arena.zones().get(args[2].toLowerCase(Locale.ROOT));
                if (zone == null) {
                    plugin.msg().send(sender, "zone-not-found", Ph.of("zone", args[2]));
                    return;
                }
                ZoneType type = ZoneType.parse(args[3]);
                if (type == null) {
                    plugin.msg().send(sender, "zone-type-unknown", Ph.of("types", ZoneType.names()));
                    return;
                }
                zone.type(type);
                plugin.arenas().save();
                plugin.msg().send(sender, "zone-type-changed",
                        Ph.of("zone", zone.name(), "type", type.name()));
            }
            case "msg" -> {
                need(args, 4);
                Zone zone = arena.zones().get(args[2].toLowerCase(Locale.ROOT));
                if (zone == null) {
                    plugin.msg().send(sender, "zone-not-found", Ph.of("zone", args[2]));
                    return;
                }
                zone.messageKey(args[3]);
                plugin.arenas().save();
                plugin.msg().send(sender, "zone-msg-set", Ph.of("zone", zone.name(), "key", args[3]));
            }
            default -> throw CommandAbort.usage();
        }
    }
}
