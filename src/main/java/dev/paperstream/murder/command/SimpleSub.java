package dev.paperstream.murder.command;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class SimpleSub implements SubCommand {

    private final String name;
    private String permission = "";
    private String usage = "";
    private String helpTopic = "";
    private boolean playerOnly = true;
    private BiConsumer<CommandSender, String[]> executor = (sender, args) -> {
    };
    private BiFunction<CommandSender, String[], List<String>> completer = (sender, args) -> List.of();

    private SimpleSub(String name) {
        this.name = name;
    }

    public static SimpleSub of(String name) {
        return new SimpleSub(name);
    }

    public SimpleSub permission(String permission) {
        this.permission = permission;
        return this;
    }

    public SimpleSub usage(String usage) {
        this.usage = usage;
        return this;
    }

    public SimpleSub topic(String helpTopic) {
        this.helpTopic = helpTopic;
        return this;
    }

    public SimpleSub console() {
        this.playerOnly = false;
        return this;
    }

    public SimpleSub run(BiConsumer<CommandSender, String[]> executor) {
        this.executor = executor;
        return this;
    }

    public SimpleSub tab(BiFunction<CommandSender, String[], List<String>> completer) {
        this.completer = completer;
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String permission() {
        return permission;
    }

    @Override
    public String usage() {
        return usage;
    }

    @Override
    public String helpTopic() {
        return helpTopic;
    }

    @Override
    public boolean playerOnly() {
        return playerOnly;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        executor.accept(sender, args);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return completer.apply(sender, args);
    }
}
