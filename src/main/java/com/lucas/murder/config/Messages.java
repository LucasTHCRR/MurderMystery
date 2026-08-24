package com.lucas.murder.config;

import com.lucas.murder.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Messages {

    private final Plugin plugin;
    private final ConfigFile file;
    private ConfigurationSection overlay;
    private String prefix = "";
    private boolean perPlayer;
    private final Map<String, ConfigurationSection> languages = new HashMap<>();
    private String defaultLanguage = "en";
    private String fallbackLanguage = "en";

    public Messages(Plugin plugin, ConfigFile file) {
        this.plugin = plugin;
        this.file = file;
        reload(null);
    }

    public void reload(ConfigurationSection languageSettings) {
        file.reload();
        this.prefix = file.raw().getString("prefix", "");
        languages.clear();
        if (languageSettings != null) {
            defaultLanguage = languageSettings.getString("default", "en").toLowerCase(Locale.ROOT);
            fallbackLanguage = languageSettings.getString("fallback", "en").toLowerCase(Locale.ROOT);
            perPlayer = languageSettings.getBoolean("per-player", false);
        }
        loadLanguageFolder();
        overlay = languages.get(defaultLanguage);
    }

    private void loadLanguageFolder() {
        File folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }

        File reference = new File(folder, "messages_en.yml");
        if (!reference.exists()) {
            try (InputStream stream = plugin.getResource("messages.yml")) {
                if (stream != null) {
                    YamlConfiguration copy = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8));
                    copy.save(reference);
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not write lang/messages_en.yml: " + exception.getMessage());
            }
        }
        File[] files = folder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File languageFile : files) {
            String code = languageFile.getName()
                    .substring("messages_".length(), languageFile.getName().length() - ".yml".length())
                    .toLowerCase(Locale.ROOT);
            languages.put(code, YamlConfiguration.loadConfiguration(languageFile));
        }
    }

    private ConfigurationSection sourceFor(CommandSender sender) {
        if (perPlayer && sender instanceof Player player) {
            String code = player.locale().getLanguage().toLowerCase(Locale.ROOT);
            ConfigurationSection section = languages.get(code);
            if (section != null) {
                return section;
            }
        }
        return overlay;
    }

    private Object rawValue(ConfigurationSection source, String key) {
        if (source != null && source.contains(key)) {
            return source.get(key);
        }
        if (file.raw().contains(key)) {
            return file.raw().get(key);
        }
        ConfigurationSection fallback = languages.get(fallbackLanguage);
        if (fallback != null && fallback.contains(key)) {
            return fallback.get(key);
        }
        return null;
    }

    public String rawString(String key) {
        Object value = rawValue(overlay, key);
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof ConfigurationSection section) {
            return section.getString("text", key);
        }
        if (value instanceof Map<?, ?> map && map.get("text") != null) {
            return String.valueOf(map.get("text"));
        }
        return value == null ? key : String.valueOf(value);
    }

    public String string(String key, Map<String, String> placeholders) {
        return Text.fill(withPrefix(rawString(key)), placeholders);
    }

    public Component get(String key, Map<String, String> placeholders) {
        return get(null, key, placeholders);
    }

    public Component get(CommandSender sender, String key, Map<String, String> placeholders) {
        Object value = rawValue(sourceFor(sender), key);
        return component(value, key, placeholders);
    }

    public List<Component> list(String key, Map<String, String> placeholders) {
        return list(null, key, placeholders);
    }

    @SuppressWarnings("unchecked")
    public List<Component> list(CommandSender sender, String key, Map<String, String> placeholders) {
        Object value = rawValue(sourceFor(sender), key);
        List<Component> out = new ArrayList<>();
        if (value instanceof List<?> raw) {
            for (Object entry : raw) {
                out.add(component(entry, key, placeholders));
            }
        } else if (value != null) {
            out.add(component(value, key, placeholders));
        }
        return out;
    }

    public List<String> rawList(String key) {
        Object value = rawValue(overlay, key);
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> raw) {
            for (Object entry : raw) {
                out.add(String.valueOf(entry));
            }
        }
        return out;
    }

    public boolean isEmpty(String key) {
        Object value = rawValue(overlay, key);
        return value == null || (value instanceof String string && string.isEmpty());
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Object value = rawValue(sourceFor(sender), key);
        if (value == null) {
            sender.sendMessage(Component.text(key));
            return;
        }
        if (value instanceof String string && string.isEmpty()) {
            return;
        }
        if (value instanceof List<?> raw) {
            for (Object entry : raw) {
                sender.sendMessage(component(entry, key, placeholders));
            }
            return;
        }
        sender.sendMessage(component(value, key, placeholders));
    }

    public void sendList(CommandSender sender, String key, Map<String, String> placeholders) {
        for (Component line : list(sender, key, placeholders)) {
            sender.sendMessage(line);
        }
    }

    private String withPrefix(String raw) {
        return raw == null ? "" : raw.replace("%prefix%", prefix);
    }

    private Component component(Object value, String key, Map<String, String> placeholders) {
        if (value == null) {
            return Component.text(key);
        }
        if (value instanceof String string) {
            return Text.of(Text.fill(withPrefix(string), placeholders));
        }
        ConfigurationSection section = asSection(value);
        if (section == null) {
            return Text.of(Text.fill(withPrefix(String.valueOf(value)), placeholders));
        }
        Component component = Text.of(Text.fill(withPrefix(section.getString("text", "")), placeholders));
        String hover = section.getString("hover");
        if (hover != null && !hover.isEmpty()) {
            Component hoverComponent = Component.empty();
            String[] lines = Text.fill(withPrefix(hover), placeholders).split("\\\\n|\n");
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    hoverComponent = hoverComponent.append(Component.newline());
                }
                hoverComponent = hoverComponent.append(Text.of(lines[i]).decoration(TextDecoration.ITALIC, false));
            }
            component = component.hoverEvent(HoverEvent.showText(hoverComponent));
        }
        String click = section.getString("click");
        String clickValue = Text.fill(section.getString("value", ""), placeholders);
        if (click != null && !clickValue.isEmpty()) {
            component = component.clickEvent(clickEvent(click, clickValue));
        }
        Object suffix = section.get("suffix");
        if (suffix != null) {
            component = component.append(component(suffix, key, placeholders));
        }
        return component;
    }

    private static ClickEvent clickEvent(String type, String value) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "SUGGEST_COMMAND" -> ClickEvent.suggestCommand(value);
            case "OPEN_URL" -> ClickEvent.openUrl(value);
            case "COPY_TO_CLIPBOARD" -> ClickEvent.copyToClipboard(value);
            default -> ClickEvent.runCommand(value);
        };
    }

    @SuppressWarnings("unchecked")
    private static ConfigurationSection asSection(Object value) {
        if (value instanceof ConfigurationSection section) {
            return section;
        }
        if (value instanceof Map<?, ?> map) {
            YamlConfiguration temporary = new YamlConfiguration();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                temporary.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            return temporary;
        }
        return null;
    }

    public String prefix() {
        return prefix;
    }
}
