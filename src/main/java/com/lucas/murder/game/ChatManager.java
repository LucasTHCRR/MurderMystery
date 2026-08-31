package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatManager implements Listener {

    private final MurderPlugin plugin;
    private final Map<UUID, Long> lastMessage = new HashMap<>();
    private final Map<UUID, String> lastText = new HashMap<>();
    private final Set<UUID> muted = new HashSet<>();

    public ChatManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void mute(UUID player) {
        muted.add(player);
    }

    public void unmute(UUID player) {
        muted.remove(player);
    }

    public boolean isMuted(UUID player) {
        return muted.contains(player);
    }

    public void clear(UUID player) {
        muted.remove(player);
        lastMessage.remove(player);
        lastText.remove(player);
    }

    public void clearAll() {
        muted.clear();
        lastMessage.clear();
        lastText.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.arenas().arenaOf(player);
        if (arena == null) {
            return;
        }
        var settings = plugin.settings();
        if (!settings.getBoolean(arena, "chat.enabled", true)) {
            return;
        }
        String scope = settings.getString(arena, "chat.scope", "ARENA").toUpperCase(Locale.ROOT);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if ("OFF".equals(scope)) {
            event.setCancelled(true);
            return;
        }
        if (muted.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (arena.state() == GameState.RUNNING && settings.getBoolean(arena, "chat.mute-during-round", false)) {
            event.setCancelled(true);
            return;
        }
        if (arena.state() == GameState.COUNTDOWN && settings.getBoolean(arena, "chat.mute-during-countdown", false)) {
            event.setCancelled(true);
            return;
        }

        long cooldown = settings.getInt(arena, "chat.cooldown-ms", 1500);
        long now = System.currentTimeMillis();
        Long previous = lastMessage.get(player.getUniqueId());
        if (previous != null && now - previous < cooldown) {
            event.setCancelled(true);
            return;
        }
        int maxLength = settings.getInt(arena, "chat.max-length", 120);
        if (maxLength > 0 && message.length() > maxLength) {
            message = message.substring(0, maxLength);
        }
        if (settings.getBoolean(arena, "chat.block-repeated", true)
                && message.equalsIgnoreCase(lastText.get(player.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }
        lastMessage.put(player.getUniqueId(), now);
        lastText.put(player.getUniqueId(), message);

        if ("GLOBAL".equals(scope)) {
            return;
        }

        event.setCancelled(true);
        GamePlayer sender = arena.participant(player.getUniqueId());
        boolean dead = sender != null && (!sender.alive() || sender.spectator());
        boolean separate = settings.getBoolean(arena, "chat.dead-separate", true);
        String displayName = plugin.nametags().chatName(arena, sender, player);
        String formatKey = dead ? "chat.spectator-format" : "chat.format";
        String format = settings.getString(arena, formatKey,
                dead ? "&8[Dead] &7%player%&8: &f%message%" : "&7%player%&8: &f%message%");

        var component = Text.of(Text.fill(format, Ph.of("player", displayName, "message", message)));
        boolean deadVisible = settings.getBoolean(arena, "chat.dead-visible-to-alive", false);

        for (Player receiver : arena.onlinePlayers()) {
            GamePlayer target = arena.participant(receiver.getUniqueId());
            boolean receiverDead = target != null && (!target.alive() || target.spectator());
            if (dead && separate && !receiverDead && !deadVisible) {
                continue;
            }
            receiver.sendMessage(component);
        }

        plugin.getServer().getConsoleSender().sendMessage(component);
    }
}
