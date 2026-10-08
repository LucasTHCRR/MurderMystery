package com.lucas.murder.command;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.npc.MurderNpc;
import com.lucas.murder.npc.NpcType;
import com.lucas.murder.npc.Skin;
import com.lucas.murder.util.Locations;
import com.lucas.murder.util.Ph;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class NpcCommands extends CommandGroup {

    public NpcCommands(MurderPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<SubCommand> build() {
        return List.of(SimpleSub.of("npc")
                .permission("murder.admin")
                .usage("/murder npc <create|remove|list|move|setname|setskin|setarena|"
                        + "sethologram|delhologram|setequip|setpose|lookat|tp|respawn|provider> ...")
                .topic("npc")
                .tab(this::complete)
                .run(this::run));
    }

    private List<String> complete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> List.of("create", "remove", "list", "move", "setname", "setskin", "setarena",
                    "sethologram", "delhologram", "setequip", "setpose", "lookat", "tp",
                    "respawn", "provider");
            case 2 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> typeNames();
                case "provider" -> new ArrayList<>(plugin.npcProviders().providers().keySet());
                default -> npcIds();
            };
            case 3 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> arenaNames();
                case "setarena" -> arenaNames();
                case "setskin" -> playerNames();
                case "setpose" -> List.of("STANDING", "SITTING", "SNEAKING", "LYING");
                case "lookat" -> List.of("PLAYER", "FIXED");
                case "setequip" -> List.of("hand", "offhand", "head", "chest", "legs", "feet");
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private List<String> typeNames() {
        List<String> names = new ArrayList<>();
        for (NpcType type : NpcType.values()) {
            if (type != NpcType.CORPSE) {
                names.add(type.name());
            }
        }
        return names;
    }

    private List<String> npcIds() {
        List<String> ids = new ArrayList<>();
        for (MurderNpc npc : plugin.npcs().all()) {
            ids.add(String.valueOf(npc.id()));
        }
        return ids;
    }

    private MurderNpc npc(CommandSender sender, String raw) {
        int id = MurderCommand.number(plugin, sender, raw);
        MurderNpc npc = plugin.npcs().get(id);
        if (npc == null) {
            plugin.msg().send(sender, "npc-not-found", Ph.of("id", id));
            throw CommandAbort.silent();
        }
        return npc;
    }

    private void run(CommandSender sender, String[] args) {
        need(args, 1);
        String mode = args[0].toLowerCase(Locale.ROOT);

        switch (mode) {
            case "create" -> {
                need(args, 2);
                Player player = player(sender);
                NpcType type = NpcType.parse(args[1]);
                if (type == null) {
                    plugin.msg().send(sender, "npc-type-unknown", Ph.of("types", NpcType.names()));
                    return;
                }
                String arenaName = null;
                if (type.needsArena()) {
                    if (args.length < 3) {
                        plugin.msg().send(sender, "npc-type-needs-arena", Ph.of("type", type.name()));
                        return;
                    }
                    arenaName = arena(sender, args[2]).name();
                }
                MurderNpc npc = plugin.npcs().create(type, player.getLocation(), arenaName);
                plugin.msg().send(sender, "npc-created", Ph.of("id", npc.id(), "type", type.name()));
                audit(sender, "npc create", type.name());
            }
            case "remove" -> {
                need(args, 2);
                MurderNpc npc = npc(sender, args[1]);
                plugin.npcs().remove(npc.id());
                plugin.msg().send(sender, "npc-removed", Ph.of("id", npc.id()));
            }
            case "list" -> {
                if (plugin.npcs().all().isEmpty()) {
                    plugin.msg().send(sender, "npc-list-empty");
                    return;
                }
                plugin.msg().send(sender, "npc-list-header");
                for (MurderNpc npc : plugin.npcs().all()) {
                    var location = npc.location();
                    plugin.msg().send(sender, "npc-list-entry", Ph.of(
                            "id", npc.id(),
                            "type", npc.type().name(),
                            "world", location.getWorld() == null ? "?" : location.getWorld().getName(),
                            "x", location.getBlockX(),
                            "y", location.getBlockY(),
                            "z", location.getBlockZ(),
                            "extra", plugin.npcs().describe(npc)));
                }
            }
            case "move" -> {
                need(args, 2);
                MurderNpc npc = npc(sender, args[1]);
                npc.location(player(sender).getLocation());
                plugin.npcs().spawn(npc);
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-moved", Ph.of("id", npc.id()));
            }
            case "setname" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                npc.name(name);
                plugin.npcProviders().active().setName(npc);
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-name-set", Ph.of("id", npc.id(), "name", name));
            }
            case "setskin" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                if ("url".equalsIgnoreCase(args[2])) {
                    need(args, 4);
                    npc.skin(new Skin(null, args[3], ""));
                    plugin.npcProviders().active().setSkin(npc);
                    plugin.npcs().save();
                    plugin.msg().send(sender, "npc-skin-set",
                            Ph.of("id", npc.id(), "player", "url"));
                    return;
                }
                String owner = args[2];
                plugin.msg().send(sender, "npc-skin-loading");
                plugin.npcs().skins().byName(owner, skin -> {
                    if (skin == null) {
                        plugin.msg().send(sender, "npc-skin-failed", Ph.of("reason", owner));
                        return;
                    }
                    npc.skin(skin);
                    plugin.npcs().spawn(npc);
                    plugin.npcs().save();
                    plugin.msg().send(sender, "npc-skin-set",
                            Ph.of("id", npc.id(), "player", owner));
                });
            }
            case "setarena" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                Arena arena = arena(sender, args[2]);
                npc.arena(arena.name());
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-arena-set",
                        Ph.of("id", npc.id(), "arena", arena.name()));
            }
            case "sethologram" -> {
                need(args, 4);
                MurderNpc npc = npc(sender, args[1]);
                int line = MurderCommand.number(plugin, sender, args[2]);
                String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                while (npc.hologram().size() < line) {
                    npc.hologram().add("");
                }
                npc.hologram().set(line - 1, text);
                plugin.npcs().save();
                plugin.npcs().spawn(npc);
                plugin.msg().send(sender, "npc-hologram-set", Ph.of("line", line, "text", text));
            }
            case "delhologram" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                int line = MurderCommand.number(plugin, sender, args[2]);
                if (line < 1 || line > npc.hologram().size()) {
                    throw CommandAbort.usage();
                }
                npc.hologram().remove(line - 1);
                plugin.npcs().save();
                plugin.npcs().spawn(npc);
                plugin.msg().send(sender, "npc-hologram-removed", Ph.of("line", line));
            }
            case "setequip" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                Player player = player(sender);
                MurderNpc.EquipSlot slot = MurderNpc.EquipSlot.parse(args[2]);
                if (slot == null) {
                    plugin.msg().send(sender, "npc-type-unknown",
                            Ph.of("types", MurderNpc.EquipSlot.names()));
                    return;
                }
                Material material = player.getInventory().getItemInMainHand().getType();
                if (material.isAir()) {
                    npc.equipment().remove(slot);
                } else {
                    npc.equipment().put(slot, material);
                }
                plugin.npcProviders().active().setEquipment(npc);
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-equip-set",
                        Ph.of("slot", slot.name(), "item", material.name()));
            }
            case "setpose" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                npc.pose(MurderNpc.Pose.parse(args[2]));
                plugin.npcProviders().active().setPose(npc);
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-pose-set",
                        Ph.of("id", npc.id(), "pose", npc.pose().name()));
            }
            case "lookat" -> {
                need(args, 3);
                MurderNpc npc = npc(sender, args[1]);
                npc.lookAtPlayers("PLAYER".equalsIgnoreCase(args[2]));
                plugin.npcs().save();
                plugin.msg().send(sender, "npc-lookat-set",
                        Ph.of("id", npc.id(), "mode", args[2].toUpperCase(Locale.ROOT)));
            }
            case "tp" -> {
                need(args, 2);
                MurderNpc npc = npc(sender, args[1]);
                player(sender).teleport(npc.location());
                plugin.msg().send(sender, "teleported",
                        Ph.of("target", "NPC #" + npc.id() + " " + Locations.pretty(npc.location())));
            }
            case "respawn" -> {
                int amount = plugin.npcs().respawnAll();
                plugin.msg().send(sender, "npc-respawned", Ph.of("amount", amount));
            }
            case "provider" -> {
                need(args, 2);
                var provider = plugin.npcProviders().provider(args[1]);
                if (provider == null) {
                    plugin.msg().send(sender, "provider-unknown",
                            Ph.of("providers", plugin.npcProviders().names()));
                    return;
                }

                if (!provider.isAvailable() || !plugin.npcProviders().switchTo(args[1])) {
                    plugin.msg().send(sender, "provider-unavailable", Ph.of(
                            "provider", provider.name(),
                            "fallback", plugin.npcProviders().active().name()));
                    return;
                }
                int amount = plugin.npcs().respawnAll();
                plugin.msg().send(sender, "provider-switched", Ph.of(
                        "type", "NPC",
                        "provider", plugin.npcProviders().active().name(),
                        "amount", amount));
                audit(sender, "npc provider", args[1]);
            }
            default -> throw CommandAbort.usage();
        }
    }
}
