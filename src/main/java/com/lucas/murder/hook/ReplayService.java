package com.lucas.murder.hook;

import com.lucas.murder.game.Arena;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface ReplayService {

    record ReplayInfo(String id, String arena, long date, int durationSeconds, List<UUID> players) {
    }

    boolean isAvailable();

    String startRecording(Arena arena);

    void stopRecording(String id, boolean save);

    boolean play(Player viewer, String id);

    void stop(Player viewer);

    List<ReplayInfo> listFor(UUID player, int limit);

    ReplayInfo info(String id);

    boolean delete(String id);

    int cleanup(int keepDays, int keepMax);
}
