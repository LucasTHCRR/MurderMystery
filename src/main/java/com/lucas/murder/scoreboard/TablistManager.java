package com.lucas.murder.scoreboard;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public final class TablistManager {

    private final MurderPlugin plugin;
    private final Placeholders placeholders;

    public TablistManager(MurderPlugin plugin, Placeholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    public void apply(Player player, Arena arena) {
        update(player, arena);
    }

    public void update(Arena arena) {
        for (Player player : arena.onlinePlayers()) {
            update(player, arena);
        }
    }

    private void update(Player player, Arena arena) {
        if (!plugin.configs().scoreboard().getBoolean("tablist.enabled", true)) {
            return;
        }
        GamePlayer participant = arena == null ? null : arena.participant(player.getUniqueId());
        Component header = join(plugin.configs().scoreboard().getStringList("tablist.header"),
                arena, participant, player);
        Component footer = join(plugin.configs().scoreboard().getStringList("tablist.footer"),
                arena, participant, player);
        player.sendPlayerListHeaderAndFooter(header, footer);

        String prefix = "";
        if (participant != null && (participant.spectator() || !participant.alive())) {
            prefix = plugin.configs().scoreboard().getString("tablist.spectator-prefix", "");
        }
        String name = plugin.nametags().tablistName(arena, participant, player);
        player.playerListName(Text.of(prefix + name));
    }

    public void clear(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        player.playerListName(null);
    }

    private Component join(List<String> lines, Arena arena, GamePlayer participant, Player player) {
        Component out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out = out.append(Component.newline());
            }
            out = out.append(Text.of(placeholders.apply(lines.get(i), arena, participant, player)));
        }
        return out;
    }
}
