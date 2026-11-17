package dev.paperstream.murder.command;

import dev.paperstream.murder.game.Arena;
import org.bukkit.entity.Player;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class SetupStep {

    private final int number;
    private final String nameKey;
    private final boolean required;
    private final Predicate<Arena> check;
    private final BiPredicate<Player, Arena> here;
    private final ToIntFunction<Arena> current;
    private final ToIntFunction<Arena> needed;

    private SetupStep(int number, String nameKey, boolean required,
                      Predicate<Arena> check, BiPredicate<Player, Arena> here,
                      ToIntFunction<Arena> current, ToIntFunction<Arena> needed) {
        this.number = number;
        this.nameKey = nameKey;
        this.required = required;
        this.check = check;
        this.here = here;
        this.current = current;
        this.needed = needed;
    }

    public static SetupStep of(int number, String nameKey, boolean required,
                               Predicate<Arena> check, BiPredicate<Player, Arena> here) {
        return new SetupStep(number, nameKey, required, check, here, null, null);
    }

    public static SetupStep counting(int number, String nameKey, boolean required,
                                     BiPredicate<Player, Arena> here,
                                     ToIntFunction<Arena> current, ToIntFunction<Arena> needed) {
        return new SetupStep(number, nameKey, required,
                arena -> current.applyAsInt(arena) >= needed.applyAsInt(arena),
                here, current, needed);
    }

    public int number() {
        return number;
    }

    public String nameKey() {
        return nameKey;
    }

    public boolean required() {
        return required;
    }

    public boolean done(Arena arena) {
        return check.test(arena);
    }

    public boolean here(Player player, Arena arena) {
        return here != null && here.test(player, arena);
    }

    public boolean counts() {
        return current != null;
    }

    public int current(Arena arena) {
        return current == null ? 0 : current.applyAsInt(arena);
    }

    public int needed(Arena arena) {
        return needed == null ? 0 : needed.applyAsInt(arena);
    }
}
