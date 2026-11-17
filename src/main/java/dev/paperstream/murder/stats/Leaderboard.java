package dev.paperstream.murder.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Leaderboard {

    public record Entry(String name, int value) {
    }

    private final StatsManager stats;

    public Leaderboard(StatsManager stats) {
        this.stats = stats;
    }

    public List<Entry> top(String stat, int limit) {
        Collection<PlayerStats> all = stats.all();
        List<PlayerStats> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Integer.compare(b.statValue(stat), a.statValue(stat)));
        List<Entry> out = new ArrayList<>();
        for (PlayerStats entry : sorted) {
            if (out.size() >= limit) {
                break;
            }
            if (entry.statValue(stat) <= 0) {
                continue;
            }
            out.add(new Entry(entry.name(), entry.statValue(stat)));
        }
        return out;
    }
}
