package com.lucas.murder.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String name();

    String permission();

    String usage();

    default String helpTopic() {
        return "";
    }

    default boolean playerOnly() {
        return true;
    }

    void execute(CommandSender sender, String[] args);

    default List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
