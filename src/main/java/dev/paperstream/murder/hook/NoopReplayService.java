package dev.paperstream.murder.hook;

import dev.paperstream.murder.game.Arena;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class NoopReplayService implements ReplayService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String startRecording(Arena arena) {
        return null;
    }

    @Override
    public void stopRecording(String id, boolean save) {
    }

    @Override
    public boolean play(Player viewer, String id) {
        return false;
    }

    @Override
    public void stop(Player viewer) {
    }

    @Override
    public List<ReplayInfo> listFor(UUID player, int limit) {
        return List.of();
    }

    @Override
    public ReplayInfo info(String id) {
        return null;
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public int cleanup(int keepDays, int keepMax) {
        return 0;
    }
}
