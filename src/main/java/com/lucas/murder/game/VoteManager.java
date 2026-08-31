package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoteManager {

    private final MurderPlugin plugin;
    private final Map<UUID, String> votes = new HashMap<>();
    private String forced;

    public VoteManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean vote(UUID player, Arena arena) {
        if (votes.containsKey(player)) {
            return false;
        }
        votes.put(player, arena.name());
        return true;
    }

    public void force(String arena) {
        this.forced = arena;
    }

    public String forced() {
        return forced;
    }

    public int votesFor(Arena arena) {
        int count = 0;
        for (String vote : votes.values()) {
            if (vote.equalsIgnoreCase(arena.name())) {
                count++;
            }
        }
        return count;
    }

    public Map<String, Integer> tally() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Arena arena : plugin.arenas().all()) {
            if (arena.enabled()) {
                counts.put(arena.name(), votesFor(arena));
            }
        }
        return counts;
    }

    public Arena winner(org.bukkit.entity.Player player) {
        if (forced != null) {
            Arena arena = plugin.arenas().get(forced);
            if (arena != null && arena.canJoin(player)) {
                return arena;
            }
        }
        Arena best = null;
        int bestVotes = -1;
        List<Arena> candidates = new ArrayList<>();
        for (Arena arena : plugin.arenas().all()) {
            if (arena.canJoin(player)) {
                candidates.add(arena);
            }
        }
        for (Arena arena : candidates) {
            int count = votesFor(arena);
            if (count > bestVotes) {
                bestVotes = count;
                best = arena;
            }
        }
        return best;
    }

    public void reset() {
        votes.clear();
        forced = null;
    }

    public void clear(UUID player) {
        votes.remove(player);
    }
}
