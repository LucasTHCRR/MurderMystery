package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HelpService {

    private static final int LINES_PER_PAGE = 8;

    private static final Map<String, String> TOPIC_PERMISSIONS = Map.of(
            "setup", "murder.admin",
            "arena", "murder.admin",
            "spawns", "murder.admin",
            "zones", "murder.admin",
            "npc", "murder.admin",
            "config", "murder.admin");

    private final MurderPlugin plugin;

    public HelpService(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendOverview(CommandSender sender) {
        plugin.msg().send(sender, "help-header");
        plugin.msg().sendList(sender, "help-player", Ph.none());
        if (sender.hasPermission("murder.admin")) {
            plugin.msg().sendList(sender, "help-admin", Ph.none());
        }
        plugin.msg().send(sender, "help-footer");
        plugin.msg().send(sender, "help-topics-hint",
                Ph.of("topics", String.join(", ", topics(sender))));
    }

    public List<String> topics(CommandSender sender) {
        List<String> out = new ArrayList<>();
        ConfigurationSection topics = plugin.configs().file("messages.yml").raw()
                .getConfigurationSection("topics");
        if (topics == null) {
            return out;
        }
        for (String key : topics.getKeys(false)) {
            String permission = TOPIC_PERMISSIONS.get(key);
            if (permission == null || sender.hasPermission(permission)) {
                out.add(key);
            }
        }
        return out;
    }

    public void sendTopic(CommandSender sender, String topic, int page) {
        String key = topic.toLowerCase(Locale.ROOT);
        List<String> lines = plugin.msg().rawList("topics." + key);
        if (lines.isEmpty()) {
            plugin.msg().send(sender, "help-topic-unknown",
                    Ph.of("topics", String.join(", ", topics(sender))));
            return;
        }
        String permission = TOPIC_PERMISSIONS.get(key);
        if (permission != null && !sender.hasPermission(permission)) {
            plugin.msg().send(sender, "help-no-permission-topic");
            return;
        }

        int pages = Math.max(1, (int) Math.ceil((double) lines.size() / LINES_PER_PAGE));
        int current = Math.min(Math.max(1, page), pages);
        plugin.msg().send(sender, "help-topic-header", Ph.of("topic", key));

        Map<String, String> placeholders = Ph.of(
                "seconds", plugin.settings().getInt(null, "gold.respawn-seconds", 20),
                "needed", plugin.settings().getInt(null, "gold.bow-at", 10));
        int from = (current - 1) * LINES_PER_PAGE;
        int to = Math.min(lines.size(), from + LINES_PER_PAGE);
        for (int i = from; i < to; i++) {
            sender.sendMessage(Text.of(Text.fill(lines.get(i), placeholders)));
        }

        if (pages > 1) {
            Component footer = Text.of(Text.fill(plugin.msg().rawString("help-topic-footer"),
                    Ph.of("page", current, "pages", pages)));
            if (current > 1) {
                footer = Text.of(plugin.msg().rawString("help-page-prev"))
                        .clickEvent(ClickEvent.runCommand("/murder help " + key + " " + (current - 1)))
                        .append(Component.space()).append(footer);
            }
            if (current < pages) {
                footer = footer.append(Component.space())
                        .append(Text.of(plugin.msg().rawString("help-page-next"))
                                .clickEvent(ClickEvent.runCommand(
                                        "/murder help " + key + " " + (current + 1))));
            }
            sender.sendMessage(footer);
        }
    }

    public void sendUsage(CommandSender sender, SubCommand sub) {
        plugin.msg().send(sender, "usage", Ph.of("usage", sub.usage()));
        if (!sub.helpTopic().isEmpty()) {
            sender.sendMessage(plugin.msg().get(sender, "usage-help-suffix",
                    Ph.of("topic", sub.helpTopic())));
        }
    }
}
