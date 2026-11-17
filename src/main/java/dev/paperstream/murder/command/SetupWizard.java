package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.npc.MurderNpc;
import dev.paperstream.murder.npc.NpcType;
import dev.paperstream.murder.util.Locations;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SetupWizard {

    private static final class Session {
        final String arena;
        int step = 1;

        Session(String arena) {
            this.arena = arena;
        }
    }

    private final MurderPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final List<SetupStep> steps = new ArrayList<>();

    public SetupWizard(MurderPlugin plugin) {
        this.plugin = plugin;
        buildSteps();
    }

    private void buildSteps() {
        steps.add(SetupStep.of(1, "setup-name-lobby", true,
                arena -> plugin.arenas().globalLobby() != null,
                (player, arena) -> {
                    plugin.arenas().globalLobby(player.getLocation());
                    return true;
                }));

        steps.add(SetupStep.of(2, "setup-name-exit", true,
                arena -> plugin.arenas().exit() != null,
                (player, arena) -> {
                    plugin.arenas().exit(player.getLocation());
                    return true;
                }));

        steps.add(SetupStep.of(3, "setup-name-arena", true,
                arena -> true,
                (player, arena) -> false));

        steps.add(SetupStep.of(4, "setup-name-bounds", true,
                arena -> arena.bounds() != null,
                (player, arena) -> {
                    var selection = plugin.selections().selection(player.getUniqueId());
                    if (selection == null) {
                        plugin.msg().send(player, "selection-missing");
                        return true;
                    }
                    arena.bounds(selection);
                    plugin.arenas().save();
                    return true;
                }));

        steps.add(SetupStep.counting(5, "setup-name-spawns", true,
                (player, arena) -> {
                    arena.spawns().add(player.getLocation());
                    plugin.arenas().save();
                    return true;
                },
                arena -> arena.spawns().size(),
                Arena::maxPlayers));

        steps.add(SetupStep.of(6, "setup-name-spectator", true,
                arena -> arena.spectatorSpawn() != null,
                (player, arena) -> {
                    arena.spectatorSpawn(player.getLocation());
                    plugin.arenas().save();
                    return true;
                }));

        steps.add(SetupStep.counting(7, "setup-name-gold", true,
                (player, arena) -> {
                    arena.goldSpawns().add(Locations.center(player.getLocation()));
                    plugin.arenas().save();
                    return true;
                },
                arena -> arena.goldSpawns().size(),
                arena -> plugin.settings().getInt(arena, "gold.min-spawns", 20)));

        steps.add(SetupStep.of(8, "setup-name-zones", false,
                arena -> !arena.zones().isEmpty(),
                (player, arena) -> false));

        steps.add(SetupStep.of(9, "setup-name-altar", false,
                arena -> arena.altar() != null,
                (player, arena) -> {
                    var block = player.getTargetBlockExact(6);
                    if (block == null) {
                        return false;
                    }
                    arena.altar(block.getLocation());
                    plugin.arenas().save();
                    return true;
                }));

        steps.add(SetupStep.of(10, "setup-name-potions", false,
                arena -> !arena.potionStations().isEmpty(),
                (player, arena) -> {
                    var block = player.getTargetBlockExact(6);
                    arena.potionStations().add(block == null ? player.getLocation() : block.getLocation());
                    plugin.arenas().save();
                    return true;
                }));

        steps.add(SetupStep.of(11, "setup-name-settings", false,
                arena -> true,
                (player, arena) -> false));

        steps.add(SetupStep.of(12, "setup-name-npc", false,
                arena -> hasJoinNpc(arena),
                (player, arena) -> {
                    plugin.npcs().create(NpcType.JOIN, player.getLocation(), arena.name());
                    return true;
                }));
    }

    private boolean hasJoinNpc(Arena arena) {
        for (MurderNpc npc : plugin.npcs().all()) {
            if (npc.type() == NpcType.JOIN && arena.name().equalsIgnoreCase(npc.arena())) {
                return true;
            }
        }
        return false;
    }

    public void handle(Player player, String[] args) {
        if (args.length == 0) {
            show(player);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(player, args);
            case "here" -> here(player);
            case "next" -> move(player, 1, true);
            case "back" -> move(player, -1, false);
            case "goto" -> {
                if (args.length < 2) {
                    throw CommandAbort.usage();
                }
                Session session = require(player);
                session.step = Math.min(steps.size(), Math.max(1,
                        MurderCommand.number(plugin, player, args[1])));
                show(player);
            }
            case "status" -> status(player);
            case "cancel" -> {
                sessions.remove(player.getUniqueId());
                plugin.msg().send(player, "setup-cancelled");
            }
            case "finish" -> finish(player);
            default -> throw CommandAbort.usage();
        }
    }

    private void start(Player player, String[] args) {
        if (args.length < 2) {
            throw CommandAbort.usage();
        }
        Arena arena = plugin.arenas().get(args[1]);
        if (arena == null) {
            arena = plugin.arenas().create(args[1]);
            plugin.msg().send(player, "arena-created", Ph.of("arena", arena.name()));
        }
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            if (entry.getValue().arena.equalsIgnoreCase(arena.name())
                    && !entry.getKey().equals(player.getUniqueId())) {
                var other = plugin.getServer().getPlayer(entry.getKey());
                plugin.msg().send(player, "setup-other-admin",
                        Ph.of("player", other == null ? "?" : other.getName()));
            }
        }
        sessions.put(player.getUniqueId(), new Session(arena.name()));
        plugin.msg().send(player, "setup-started", Ph.of("arena", arena.name()));
        show(player);
    }

    private Session require(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            plugin.msg().send(player, "setup-not-active");
            throw CommandAbort.silent();
        }
        return session;
    }

    private void here(Player player) {
        Session session = require(player);
        Arena arena = plugin.arenas().get(session.arena);
        if (arena == null) {
            sessions.remove(player.getUniqueId());
            plugin.msg().send(player, "setup-not-active");
            return;
        }
        SetupStep step = steps.get(session.step - 1);
        if (!step.here(player, arena)) {
            plugin.msg().send(player, "setup-here-nothing");
            return;
        }
        plugin.sounds().play(player, "setup-step");

        if (step.counts()) {
            int missing = Math.max(0, step.needed(arena) - step.current(arena));
            plugin.msg().send(player, "setup-step-done", Ph.of(
                    "what", plugin.msg().rawString(step.nameKey()) + " #" + step.current(arena),
                    "missing", missing));
            if (missing > 0) {
                actionbar(player, session);
                return;
            }
        } else {
            plugin.msg().send(player, "setup-step-complete", Ph.of("step", step.number()));
        }
        move(player, 1, false);
    }

    private void move(Player player, int delta, boolean announceSkip) {
        Session session = require(player);
        int target = session.step + delta;
        if (target < 1) {
            plugin.msg().send(player, "setup-first-step");
            return;
        }
        if (target > steps.size()) {
            plugin.msg().send(player, "setup-last-step");
            return;
        }
        if (announceSkip) {
            plugin.msg().send(player, "setup-step-skipped", Ph.of("step", session.step));
        }
        session.step = target;
        show(player);
    }

    private void show(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            plugin.msg().send(player, "setup-not-active");
            return;
        }
        Arena arena = plugin.arenas().get(session.arena);
        if (arena == null) {
            sessions.remove(player.getUniqueId());
            plugin.msg().send(player, "setup-not-active");
            return;
        }
        SetupStep step = steps.get(session.step - 1);
        Map<String, String> placeholders = Ph.of(
                "arena", arena.name(),
                "step", session.step,
                "total", steps.size(),
                "title", plugin.msg().rawString("setup-step-" + step.number() + "-title"),
                "seconds", plugin.settings().getInt(arena, "gold.respawn-seconds", 20));

        plugin.msg().sendList(player, "setup-step-header", placeholders);
        String text = plugin.msg().rawString("setup-step-" + step.number() + "-text");
        player.sendMessage(Text.of("&7   " + Text.fill(text, placeholders)));
        if (step.counts()) {
            player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-step-progress"),
                    Ph.of("current", step.current(arena), "needed", step.needed(arena)))));
        }

        Component buttons = plugin.msg().get(player, "setup-button-here", placeholders)
                .append(plugin.msg().get(player, "setup-button-next", placeholders))
                .append(plugin.msg().get(player, "setup-button-back", placeholders))
                .append(plugin.msg().get(player, "setup-button-status", placeholders));
        player.sendMessage(buttons);

        String hint = plugin.msg().rawString("setup-step-" + step.number() + "-hint");
        player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-step-hint"),
                Ph.of("hint", Text.fill(hint, placeholders)))));
        plugin.msg().send(player, "setup-step-footer");
        actionbar(player, session);
    }

    private void actionbar(Player player, Session session) {
        player.sendActionBar(plugin.msg().get(player, "setup-actionbar", Ph.of(
                "arena", session.arena, "step", session.step, "total", steps.size())));
    }

    private void status(Player player) {
        Session session = require(player);
        Arena arena = plugin.arenas().get(session.arena);
        if (arena == null) {
            return;
        }
        plugin.msg().send(player, "setup-status-header", Ph.of("arena", arena.name()));
        int missing = 0;
        for (SetupStep step : steps) {
            String name = plugin.msg().rawString(step.nameKey());
            String extra = step.counts()
                    ? step.current(arena) + "/" + step.needed(arena) : "";
            if (step.done(arena)) {
                player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-status-ok"),
                        Ph.of("step", step.number(), "name", name))));
            } else if (step.required()) {
                missing++;
                player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-status-missing"),
                        Ph.of("step", step.number(), "name", name, "extra", extra))));
            } else {
                player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-status-optional"),
                        Ph.of("step", step.number(), "name", name))));
            }
        }
        player.sendMessage(Text.of(Text.fill(plugin.msg().rawString("setup-status-footer"),
                Ph.of("amount", missing))));
        player.sendMessage(plugin.msg().get(player, "setup-button-continue", Ph.none()));
    }

    private void finish(Player player) {
        Session session = require(player);
        Arena arena = plugin.arenas().get(session.arena);
        if (arena == null) {
            return;
        }
        int missing = 0;
        for (SetupStep step : steps) {
            if (step.required() && !step.done(arena)) {
                missing++;
            }
        }
        if (missing > 0) {
            plugin.msg().send(player, "setup-finish-missing", Ph.of("amount", missing));
            return;
        }
        arena.enabled(true);
        plugin.arenas().save();
        sessions.remove(player.getUniqueId());
        plugin.msg().send(player, "setup-finished", Ph.of("arena", arena.name()));
        plugin.audit().write(player, "setup finish", arena.name());
    }

    public void clear(UUID player) {
        sessions.remove(player);
    }
}
