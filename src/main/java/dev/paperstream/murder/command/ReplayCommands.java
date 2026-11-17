package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.hook.ReplayService;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ReplayCommands extends CommandGroup {

    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReplayCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        return List.of(SimpleSub.of("replay")
                .permission("murder.replay")
                .usage("/murder replay <last|list|play|stop|record|stoprecord|delete|cleanup> [id]")
                .topic("replay")
                .tab((sender, args) -> args.length == 1
                        ? List.of("last", "list", "play", "stop", "record", "stoprecord",
                        "delete", "cleanup")
                        : List.of())
                .run(this::run));
    }

    private void run(CommandSender sender, String[] args) {
        need(args, 1);
        Player player = player(sender);
        ReplayService replays = plugin.replays();

        if (!plugin.settings().feature("replays")) {
            plugin.msg().send(sender, "replay-disabled");
            return;
        }
        if (!replays.isAvailable()) {
            plugin.msg().send(sender, "replay-not-installed");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "last" -> {
                List<ReplayService.ReplayInfo> own = replays.listFor(player.getUniqueId(), 1);
                if (own.isEmpty()) {
                    plugin.msg().send(sender, "replay-none");
                    return;
                }
                play(player, own.get(0).id());
            }
            case "list" -> {
                List<ReplayService.ReplayInfo> own = replays.listFor(player.getUniqueId(), 10);
                if (own.isEmpty()) {
                    plugin.msg().send(sender, "replay-list-empty");
                    return;
                }
                plugin.msg().send(sender, "replay-list-header");
                for (ReplayService.ReplayInfo info : own) {
                    plugin.msg().send(sender, "replay-list-entry", Ph.of(
                            "id", info.id(),
                            "arena", info.arena(),
                            "date", DATE.format(new Date(info.date())),
                            "duration", Text.duration(info.durationSeconds())));
                }
            }
            case "play" -> {
                need(args, 2);
                play(player, args[1]);
            }
            case "stop" -> {
                replays.stop(player);
                plugin.msg().send(sender, "replay-stopped");
            }
            case "record" -> {
                requireAdmin(sender);
                need(args, 2);
                Arena arena = arena(sender, args[1]);
                String id = replays.startRecording(arena);
                if (id == null) {
                    plugin.msg().send(sender, "replay-recording-failed", Ph.of("reason", arena.name()));
                    return;
                }
                plugin.msg().send(sender, "replay-recording-started", Ph.of("id", id));
            }
            case "stoprecord" -> {
                requireAdmin(sender);
                need(args, 2);
                Arena arena = arena(sender, args[1]);
                if (arena.replayId() == null) {
                    plugin.msg().send(sender, "replay-not-found", Ph.of("id", "-"));
                    return;
                }
                replays.stopRecording(arena.replayId(), true);
                plugin.msg().send(sender, "replay-recording-stopped",
                        Ph.of("id", arena.replayId(), "seconds", 0));
            }
            case "delete" -> {
                requireAdmin(sender);
                need(args, 2);
                if (!replays.delete(args[1])) {
                    plugin.msg().send(sender, "replay-not-found", Ph.of("id", args[1]));
                    return;
                }
                plugin.msg().send(sender, "replay-deleted", Ph.of("id", args[1]));
            }
            case "cleanup" -> {
                requireAdmin(sender);
                int removed = replays.cleanup(
                        plugin.settings().getInt(null, "replay.keep-days", 3),
                        plugin.settings().getInt(null, "replay.keep-max", 200));
                plugin.msg().send(sender, "replay-cleanup", Ph.of("amount", removed, "mb", 0));
            }
            default -> throw CommandAbort.usage();
        }
    }

    private void requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("murder.replay.admin")) {
            plugin.msg().send(sender, "no-permission");
            throw CommandAbort.silent();
        }
    }

    private void play(Player player, String id) {
        // Ein Replay während der Runde würde den Murderer verraten

        Arena arena = plugin.arenas().arenaOf(player);
        if (arena != null && arena.state() != GameState.WAITING) {
            plugin.msg().send(player, "replay-blocked-ingame");
            return;
        }
        ReplayService.ReplayInfo info = plugin.replays().info(id);
        if (info == null) {
            plugin.msg().send(player, "replay-not-found", Ph.of("id", id));
            return;
        }
        if (!info.players().contains(player.getUniqueId())
                && !player.hasPermission("murder.replay.others")) {
            plugin.msg().send(player, "replay-no-access");
            return;
        }
        plugin.msg().send(player, "replay-loading");
        if (plugin.replays().play(player, id)) {
            plugin.msg().send(player, "replay-started");
        } else {
            plugin.msg().send(player, "replay-recording-failed", Ph.of("reason", id));
        }
    }
}
