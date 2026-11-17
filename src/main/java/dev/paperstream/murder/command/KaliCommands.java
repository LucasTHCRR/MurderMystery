package dev.paperstream.murder.command;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.game.Arena;
import dev.paperstream.murder.game.GamePlayer;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KaliCommands extends CommandGroup {

    public KaliCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        List<SubCommand> list = new ArrayList<>();

        list.add(SimpleSub.of("kali")
                .permission("murder.admin")
                .usage("/murder kali <test|chances|favor|reload> ...")
                .topic("kali")
                .console()
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> List.of("test", "chances", "favor", "reload");
                    case 2 -> "test".equalsIgnoreCase(args[0])
                            ? List.of("blessing", "curse")
                            : ("favor".equalsIgnoreCase(args[0]) ? playerNames() : List.of());
                    case 3 -> "test".equalsIgnoreCase(args[0]) ? entries(args[1]) : List.of();
                    default -> List.of();
                })
                .run(this::kali));

        list.add(SimpleSub.of("corpse")
                .permission("murder.admin")
                .usage("/murder corpse provider <name>")
                .console()
                .tab((sender, args) -> switch (args.length) {
                    case 1 -> List.of("provider");
                    case 2 -> new ArrayList<>(plugin.corpses().providers().keySet());
                    default -> List.of();
                })
                .run((sender, args) -> {
                    need(args, 2);
                    if (!"provider".equalsIgnoreCase(args[0])) {
                        throw CommandAbort.usage();
                    }
                    var provider = plugin.corpses().provider(args[1]);
                    if (provider == null) {
                        plugin.msg().send(sender, "provider-unknown",
                                Ph.of("providers", plugin.corpses().names()));
                        return;
                    }

                    if (!provider.isAvailable()) {
                        plugin.msg().send(sender, "provider-unavailable", Ph.of(
                                "provider", provider.name(),
                                "fallback", plugin.corpses().active().name()));
                        return;
                    }
                    if (!plugin.corpses().switchTo(args[1])) {
                        plugin.msg().send(sender, "provider-unavailable", Ph.of(
                                "provider", provider.name(),
                                "fallback", plugin.corpses().active().name()));
                        return;
                    }
                    plugin.msg().send(sender, "provider-switched", Ph.of(
                            "type", "Corpse",
                            "provider", plugin.corpses().active().name(),
                            "amount", 0));
                    audit(sender, "corpse provider", args[1]);
                }));

        return list;
    }

    private List<String> entries(String table) {
        ConfigurationSection section = plugin.configs().kali()
                .getConfigurationSection("kali." + ("blessing".equalsIgnoreCase(table)
                        ? "blessings" : "curses"));
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private void kali(CommandSender sender, String[] args) {
        need(args, 1);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "chances" -> {
                plugin.msg().send(sender, "kali-chances-header");
                for (String table : List.of("blessings", "curses")) {
                    ConfigurationSection section = plugin.configs().kali()
                            .getConfigurationSection("kali." + table);
                    if (section == null) {
                        continue;
                    }
                    for (String key : section.getKeys(false)) {
                        ConfigurationSection entry = section.getConfigurationSection(key);
                        if (entry == null || !entry.getBoolean("enabled", true)) {
                            continue;
                        }
                        plugin.msg().send(sender, "kali-chances-entry", Ph.of(
                                "name", Text.plain(entry.getString("name", key)),
                                "chance", entry.getDouble("chance", 0)));
                    }
                }
                plugin.kali().chanceSums().forEach((table, sum) ->
                        plugin.msg().send(sender, "kali-chances-sum",
                                Ph.of("sum", String.format(Locale.ROOT, "%.1f", sum), "type", table)));
            }
            case "favor" -> {
                need(args, 2);
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.msg().send(sender, "player-not-found", Ph.of("player", args[1]));
                    return;
                }
                if (args.length < 3) {
                    int own = plugin.kali().favor(target.getUniqueId());
                    plugin.msg().send(sender, "kali-favor", Ph.of(
                            "favor", own,
                            "max", plugin.configs().kali().getInt("kali.max-favor", 20),
                            "chance", String.format(Locale.ROOT, "%.0f",
                                    plugin.kali().blessingChance(own))));
                    return;
                }
                int value = MurderCommand.number(plugin, sender, args[2]);
                plugin.kali().favor(target.getUniqueId(), value);
                plugin.msg().send(sender, "kali-favor-set",
                        Ph.of("player", target.getName(), "favor", value));
                audit(sender, "kali favor", target.getName() + " = " + value);
            }
            case "test" -> {
                need(args, 3);
                Player player = player(sender);
                Arena arena = plugin.arenas().arenaOf(player);
                if (arena == null) {
                    plugin.msg().send(sender, "not-in-game");
                    return;
                }
                GamePlayer participant = arena.participant(player.getUniqueId());
                boolean blessing = "blessing".equalsIgnoreCase(args[1]);
                ConfigurationSection entry = plugin.configs().kali().getConfigurationSection(
                        "kali." + (blessing ? "blessings" : "curses") + "." + args[2]);
                if (entry == null || participant == null) {
                    throw CommandAbort.usage();
                }
                plugin.kali().apply(arena, participant, player, entry, args[2], blessing);
                audit(sender, "kali test", args[1] + " " + args[2]);
            }
            case "reload" -> {
                plugin.configs().reload("kali.yml");
                plugin.msg().send(sender, "reload-file", Ph.of("file", "kali.yml"));
            }
            default -> throw CommandAbort.usage();
        }
    }
}
