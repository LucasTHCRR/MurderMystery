package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.stats.PlayerStats;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlayerCommands extends CommandGroup {

    public PlayerCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("join")
                .permission("murder.play")
                .usage("/murder join [arena]")
                .topic("arena")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run(this::join));

        list.add(SimpleSub.of("leave")
                .permission("murder.play")
                .usage("/murder leave")
                .run((sender, args) -> {
                    Player player = player(sender);
                    Arena arena = plugin.arenas().arenaOf(player);
                    if (arena == null) {
                        plugin.msg().send(sender, "not-in-game");
                        return;
                    }
                    arena.leave(player, false);
                }));

        list.add(SimpleSub.of("role")
                .permission("murder.play")
                .usage("/murder role")
                .run((sender, args) -> {
                    Player player = player(sender);
                    GamePlayer participant = plugin.arenas().participantOf(player);
                    if (participant == null) {
                        plugin.msg().send(sender, "not-in-game");
                        return;
                    }
                    if (participant.role() == null) {
                        plugin.msg().send(sender, "role-not-assigned");
                        return;
                    }
                    String role = plugin.msg().rawString(participant.role().messageKey());
                    plugin.msg().send(sender, participant.alive() ? "role-command" : "role-command-dead",
                            Ph.of("role", role));
                }));

        list.add(SimpleSub.of("list")
                .permission("murder.play")
                .usage("/murder list")
                .console()
                .run((sender, args) -> {
                    if (plugin.arenas().all().isEmpty()) {
                        plugin.msg().send(sender, "arena-list-empty");
                        return;
                    }
                    plugin.msg().send(sender, "arena-list-header");
                    for (Arena arena : plugin.arenas().all()) {
                        plugin.msg().send(sender, "arena-list-entry", Ph.of(
                                "arena", arena.name(),
                                "state", plugin.msg().rawString(arena.state().messageKey()),
                                "current", arena.playerCount(),
                                "max", arena.maxPlayers()));
                    }
                }));

        list.add(SimpleSub.of("arenas")
                .permission("murder.admin")
                .usage("/murder arenas")
                .console()
                .run((sender, args) -> plugin.getServer().dispatchCommand(sender, "murder list")));

        list.add(SimpleSub.of("vote")
                .permission("murder.play")
                .usage("/murder vote <map>")
                .topic("arena")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    Player player = player(sender);
                    if (!plugin.settings().feature("map-voting")) {
                        plugin.msg().send(sender, "feature-disabled");
                        return;
                    }
                    if (args.length == 0) {
                        plugin.menus().openVote(player);
                        return;
                    }
                    Arena arena = arena(sender, args[0]);
                    if (!plugin.votes().vote(player.getUniqueId(), arena)) {
                        plugin.msg().send(sender, "vote-already");
                        return;
                    }
                    plugin.msg().send(sender, "vote-success",
                            Ph.of("map", Text.plain(arena.displayName())));
                }));

        list.add(SimpleSub.of("shop")
                .permission("murder.shop")
                .usage("/murder shop")
                .run((sender, args) -> plugin.menus().openShop(player(sender))));

        list.add(SimpleSub.of("stats")
                .permission("murder.stats")
                .usage("/murder stats [player]")
                .console()
                .tab((sender, args) -> args.length == 1 ? playerNames() : List.of())
                .run(this::stats));

        list.add(SimpleSub.of("spectate")
                .permission("murder.spectate")
                .usage("/murder spectate <arena>")
                .tab((sender, args) -> args.length == 1 ? arenaNames() : List.of())
                .run((sender, args) -> {
                    Player player = player(sender);
                    if (!plugin.settings().feature("spectator-mode")) {
                        plugin.msg().send(sender, "feature-disabled");
                        return;
                    }
                    if (args.length == 0) {
                        plugin.menus().openSpectate(player);
                        return;
                    }
                    Arena arena = arena(sender, args[0]);
                    if (plugin.arenas().arenaOf(player) != null) {
                        plugin.msg().send(sender, "already-in-game");
                        return;
                    }
                    if (arena.state() != GameState.RUNNING) {
                        plugin.msg().send(sender, "arena-not-found", Ph.of("arena", args[0]));
                        return;
                    }
                    arena.addSpectator(player);
                }));

        list.add(SimpleSub.of("scoreboard")
                .permission("murder.play")
                .usage("/murder scoreboard")
                .run((sender, args) -> plugin.boards().toggle(player(sender))));

        return list;
    }

    private void join(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (plugin.arenas().arenaOf(player) != null) {
            plugin.msg().send(sender, "already-in-game");
            return;
        }
        long cooldown = plugin.settings().getInt(null, "join-cooldown-seconds", 3) * 1000L;
        if (!plugin.cooldowns().tryUse(player.getUniqueId(), "join", cooldown)) {
            plugin.msg().send(sender, "join-cooldown",
                    Ph.of("seconds", plugin.cooldowns().remainingSeconds(player.getUniqueId(), "join")));
            return;
        }
        Arena arena;
        if (args.length > 0) {
            arena = arena(sender, args[0]);
        } else {
            arena = plugin.votes().winner(player);
            if (arena == null) {
                arena = plugin.arenas().firstFree(player);
            }
            if (arena == null) {
                plugin.msg().send(sender, "arena-none-free");
                return;
            }
        }
        if (!arena.enabled()) {
            plugin.msg().send(sender, "arena-disabled-join");
            return;
        }
        if (!arena.state().joinable()) {
            plugin.msg().send(sender, "arena-running");
            return;
        }
        if (arena.playerCount() >= arena.maxPlayers() && !player.hasPermission("murder.join.full")) {
            plugin.msg().send(sender, "arena-full");
            return;
        }
        if (arena.spawns().isEmpty()) {
            plugin.msg().send(sender, "arena-no-spawns", Ph.of("arena", arena.name()));
            return;
        }
        if (arena.waitingLobby() == null) {
            plugin.msg().send(sender, "no-lobby-set");
            return;
        }
        if (exceedsAltLimit(arena, player)) {
            plugin.msg().send(sender, "alt-limit");
            return;
        }
        arena.join(player);
    }

    private boolean exceedsAltLimit(Arena arena, Player player) {
        int limit = plugin.settings().getInt(arena, "extras.max-accounts-per-ip", 0);
        if (limit <= 0 || player.getAddress() == null) {
            return false;
        }
        String address = player.getAddress().getAddress().getHostAddress();
        int count = 0;
        for (Player other : arena.onlinePlayers()) {
            if (other.getAddress() != null
                    && address.equals(other.getAddress().getAddress().getHostAddress())) {
                count++;
            }
        }
        return count >= limit;
    }

    private void stats(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length > 0) {
            target = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (target == null) {
                plugin.msg().send(sender, "player-not-found", Ph.of("player", args[0]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            throw CommandAbort.usage();
        }
        PlayerStats stats = plugin.stats().get(target.getUniqueId());
        plugin.msg().send(sender, "stats-header", Ph.of("player", stats.name()));
        for (String line : plugin.menus().statLines(stats)) {
            sender.sendMessage(Text.of(line));
        }
    }
}
