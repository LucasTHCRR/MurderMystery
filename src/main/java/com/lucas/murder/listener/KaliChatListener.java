package com.lucas.murder.listener;

import com.lucas.murder.MurderPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class KaliChatListener implements Listener {

    private final MurderPlugin plugin;

    public KaliChatListener(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.kali().handlePraise(event.getPlayer().getUniqueId(), message);
    }
}
