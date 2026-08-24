package com.lucas.murder.util;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AuditLog {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Plugin plugin;
    private final File file;
    private final boolean enabled;

    public AuditLog(Plugin plugin, boolean enabled, String path) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.file = new File(plugin.getDataFolder(), path);
    }

    public void write(CommandSender sender, String action, String detail) {
        if (!enabled) {
            return;
        }
        String line = "[" + LocalDateTime.now().format(FORMAT) + "] "
                + sender.getName() + " -> " + action
                + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")")
                + System.lineSeparator();
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            Files.writeString(file.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not write the audit log: " + exception.getMessage());
        }
    }
}
