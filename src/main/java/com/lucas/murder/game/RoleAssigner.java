package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoleAssigner {

    private final MurderPlugin plugin;

    private final Map<String, Set<UUID>> recentMurderers = new HashMap<>();

    private final Map<UUID, Role> forced = new HashMap<>();

    public RoleAssigner(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void force(UUID player, Role role) {
        forced.put(player, role);
    }

    public Role forcedRole(UUID player) {
        return forced.get(player);
    }

    public void clearForced(UUID player) {
        forced.remove(player);
    }

    public void assign(Arena arena) {
        List<GamePlayer> pool = new ArrayList<>(arena.participants());
        pool.removeIf(GamePlayer::spectator);
        if (pool.isEmpty()) {
            return;
        }

        int total = pool.size();
        int murderers = arena.forcedMurderers() > 0
                ? arena.forcedMurderers()
                : Math.max(1, ceilDiv(total, plugin.settings().getInt(arena, "roles.players-per-murderer", 8)));
        int detectives = arena.forcedDetectives() >= 0
                ? arena.forcedDetectives()
                : Math.max(1, ceilDiv(total, plugin.settings().getInt(arena, "roles.players-per-detective", 8)));

        if (total < 3) {
            murderers = 1;
            detectives = 0;
        }
        murderers = Math.min(murderers, Math.max(1, total - 1));
        detectives = Math.min(detectives, Math.max(0, total - murderers - 1));

        List<GamePlayer> remaining = new ArrayList<>(pool);
        List<GamePlayer> chosenMurderers = new ArrayList<>();
        List<GamePlayer> chosenDetectives = new ArrayList<>();

        for (GamePlayer participant : new ArrayList<>(remaining)) {
            Role role = forced.remove(participant.uuid());
            if (role == null) {
                continue;
            }
            if (role == Role.MURDERER && chosenMurderers.size() < murderers) {
                chosenMurderers.add(participant);
                remaining.remove(participant);
            } else if (role == Role.DETECTIVE && chosenDetectives.size() < detectives) {
                chosenDetectives.add(participant);
                remaining.remove(participant);
            } else if (role == Role.INNOCENT) {
                participant.role(Role.INNOCENT);
                remaining.remove(participant);
            }
        }

        Collections.shuffle(remaining);

        boolean avoidRepeat = plugin.settings().getBoolean(arena, "roles.avoid-repeat-murderer", true);
        Set<UUID> recent = recentMurderers.computeIfAbsent(arena.name(), key -> new HashSet<>());
        if (avoidRepeat) {
            List<GamePlayer> fresh = new ArrayList<>();
            List<GamePlayer> used = new ArrayList<>();
            for (GamePlayer participant : remaining) {
                (recent.contains(participant.uuid()) ? used : fresh).add(participant);
            }
            if (fresh.size() < murderers - chosenMurderers.size()) {

                recent.clear();
            } else {
                remaining = new ArrayList<>(fresh);
                remaining.addAll(used);
            }
        }

        while (chosenMurderers.size() < murderers && !remaining.isEmpty()) {
            chosenMurderers.add(remaining.remove(0));
        }
        Collections.shuffle(remaining);
        while (chosenDetectives.size() < detectives && !remaining.isEmpty()) {
            chosenDetectives.add(remaining.remove(0));
        }

        recent.clear();
        for (GamePlayer murderer : chosenMurderers) {
            murderer.role(Role.MURDERER);
            recent.add(murderer.uuid());
        }
        for (GamePlayer detective : chosenDetectives) {
            detective.role(Role.DETECTIVE);
        }
        for (GamePlayer participant : pool) {
            if (participant.role() == null) {
                participant.role(Role.INNOCENT);
            }
        }
    }

    public void forget(String arena) {
        recentMurderers.remove(arena);
    }

    private static int ceilDiv(int value, int divisor) {
        if (divisor <= 0) {
            return 1;
        }
        return (value + divisor - 1) / divisor;
    }
}
