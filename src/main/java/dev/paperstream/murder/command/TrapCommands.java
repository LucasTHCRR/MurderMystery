package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.region.Cuboid;
import dev.paperstream.murder.region.Trap;
import dev.paperstream.murder.util.Ph;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TrapCommands extends CommandGroup {

    private static final List<String> MODES = List.of("all", "innocents", "murderers");

    public TrapCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();
        list.add(SimpleSub.of("trap")
                .permission("murder.admin")
                .usage("/murder trap <create|remove|list|mode|cost|duration|cooldown> <name> [value]")
                .topic("zones")
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> List.of("create", "remove", "list", "mode", "cost", "duration", "cooldown");
                    case 2 -> "create".equalsIgnoreCase(args[0]) ? List.of() : trapNames(sender);
                    case 3 -> "create".equalsIgnoreCase(args[0]) || "mode".equalsIgnoreCase(args[0])
                            ? MODES : List.of();
                    default -> List.of();
                })
                .run(this::trap));
        return list;
    }

    private Arena here(CommandSender sender) {
        Player player = player(sender);
        Arena joined = plugin.arenas().arenaOf(player);
        if (joined != null) {
            return joined;
        }
        for (Arena arena : plugin.arenas().all()) {
            if (arena.bounds() != null && arena.bounds().contains(player.getLocation())) {
                return arena;
            }
        }
        plugin.msg().send(sender, "trap-no-arena");
        throw CommandAbort.silent();
    }

    private List<String> trapNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        for (Arena arena : plugin.arenas().all()) {
            if (arena.bounds() != null && arena.bounds().contains(player.getLocation())) {
                return new ArrayList<>(arena.traps().keySet());
            }
        }
        return List.of();
    }

    private Trap trapOf(CommandSender sender, Arena arena, String name) {
        Trap trap = arena.traps().get(name.toLowerCase(Locale.ROOT));
        if (trap == null) {
            plugin.msg().send(sender, "trap-not-found", Ph.of("trap", name));
            throw CommandAbort.silent();
        }
        return trap;
    }

    private int number(CommandSender sender, String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value >= 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        plugin.msg().send(sender, "trap-bad-number", Ph.of("value", raw));
        throw CommandAbort.silent();
    }

    private void trap(CommandSender sender, String[] args) {
        need(args, 1);
        String action = args[0].toLowerCase(Locale.ROOT);
        Arena arena = here(sender);

        switch (action) {
            case "create" -> {
                need(args, 2);
                Player player = player(sender);
                String name = args[1].toLowerCase(Locale.ROOT);
                if (arena.traps().containsKey(name)) {
                    plugin.msg().send(sender, "trap-exists", Ph.of("trap", name));
                    return;
                }
                Trap.Mode mode = Trap.Mode.ALL;
                if (args.length > 2) {
                    mode = Trap.Mode.parse(args[2]);
                    if (mode == null) {
                        plugin.msg().send(sender, "trap-mode-unknown", Ph.of("modes", String.join(", ", MODES)));
                        return;
                    }
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || !(target.getType() == org.bukkit.Material.LEVER
                        || Tag.BUTTONS.isTagged(target.getType()))) {
                    plugin.msg().send(sender, "trap-look-at-lever");
                    return;
                }
                Cuboid selection = plugin.selections().selection(player.getUniqueId());
                if (selection == null || plugin.selections().mismatchedWorlds(player.getUniqueId())) {
                    plugin.msg().send(sender, "selection-missing");
                    return;
                }
                arena.traps().put(name, new Trap(name, mode, selection,
                        target.getX(), target.getY(), target.getZ(), 1, 3, 30));
                plugin.arenas().save();
                plugin.msg().send(sender, "trap-created", Ph.of(
                        "trap", name, "mode", mode.name(), "arena", arena.name(), "size", selection.size()));
            }
            case "remove" -> {
                need(args, 2);
                Trap trap = trapOf(sender, arena, args[1]);
                arena.traps().remove(trap.name());
                plugin.arenas().save();
                plugin.msg().send(sender, "trap-removed", Ph.of("trap", trap.name()));
            }
            case "list" -> {
                if (arena.traps().isEmpty()) {
                    plugin.msg().send(sender, "trap-list-empty", Ph.of("arena", arena.name()));
                    return;
                }
                plugin.msg().send(sender, "trap-list-header", Ph.of("arena", arena.name()));
                for (Trap trap : arena.traps().values()) {
                    plugin.msg().send(sender, "trap-list-entry", Ph.of(
                            "trap", trap.name(), "mode", trap.mode().name(), "cost", trap.cost(),
                            "duration", trap.duration(), "cooldown", trap.cooldown()));
                }
            }
            case "mode" -> {
                need(args, 3);
                Trap trap = trapOf(sender, arena, args[1]);
                Trap.Mode mode = Trap.Mode.parse(args[2]);
                if (mode == null) {
                    plugin.msg().send(sender, "trap-mode-unknown", Ph.of("modes", String.join(", ", MODES)));
                    return;
                }
                trap.mode(mode);
                plugin.arenas().save();
                plugin.msg().send(sender, "trap-changed", Ph.of("trap", trap.name(), "key", "mode", "value", mode.name()));
            }
            case "cost", "duration", "cooldown" -> {
                need(args, 3);
                Trap trap = trapOf(sender, arena, args[1]);
                int value = number(sender, args[2]);
                switch (action) {
                    case "cost" -> trap.cost(value);
                    case "duration" -> trap.duration(Math.max(1, value));
                    default -> trap.cooldown(value);
                }
                plugin.arenas().save();
                plugin.msg().send(sender, "trap-changed", Ph.of("trap", trap.name(), "key", action, "value", value));
            }
            default -> throw CommandAbort.usage();
        }
    }
}
