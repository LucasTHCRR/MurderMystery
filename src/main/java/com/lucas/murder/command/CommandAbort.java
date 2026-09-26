package com.lucas.murder.command;

public final class CommandAbort extends RuntimeException {

    private final boolean showUsage;

    private CommandAbort(boolean showUsage) {
        super(null, null, false, false);
        this.showUsage = showUsage;
    }

    public static CommandAbort usage() {
        return new CommandAbort(true);
    }

    public static CommandAbort silent() {
        return new CommandAbort(false);
    }

    public boolean showUsage() {
        return showUsage;
    }
}
