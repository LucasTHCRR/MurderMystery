package dev.paperstream.murder.scoreboard;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.game.GameState;
import dev.paperstream.murder.util.Text;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class ActionbarManager {

    private final MurderPlugin plugin;
    private final Placeholders placeholders;

    public ActionbarManager(MurderPlugin plugin, Placeholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    public void update(Player player, Arena arena) {
        if (!plugin.settings().feature("actionbar")
                || !plugin.configs().scoreboard().getBoolean("actionbar.enabled", true)) {
            return;
        }
        if (arena.state() != GameState.RUNNING) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null) {
            return;
        }
        String path;
        if (participant.spectator() || !participant.alive()) {
            path = "actionbar.spectator";
        } else if (participant.role() == null) {
            return;
        } else {
            path = "actionbar.ingame." + participant.role().name().toLowerCase(Locale.ROOT);
        }
        String raw = plugin.configs().scoreboard().getString(path);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        player.sendActionBar(Text.of(placeholders.apply(raw, arena, participant, player)));
    }
}
