package com.lucas.murder.stats;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.Role;
import com.lucas.murder.game.WinChecker;
import com.lucas.murder.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class RewardService {

    private final MurderPlugin plugin;

    public RewardService(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.settings().getBoolean(null, "rewards.enabled", true);
    }

    private int coins(String path) {
        return plugin.settings().getInt(null, "rewards.coins." + path, 0);
    }

    public void onKill(Player killer) {
        if (!enabled()) {
            return;
        }
        give(killer, coins("per-kill"), plugin.msg().rawString("stats-kills"));
    }

    public void onRoundEnd(Arena arena, WinChecker.Result result) {
        if (!enabled() || result == WinChecker.Result.ABORTED) {
            return;
        }
        int aliveInnocents = arena.aliveOf(Role.INNOCENT).size() + arena.aliveOf(Role.DETECTIVE).size();
        for (GamePlayer participant : arena.participants()) {
            if (participant.spectator() || participant.role() == null) {
                continue;
            }
            Player player = participant.player();
            int amount = coins("per-game") + participant.gold() * coins("per-gold-collected");
            boolean won = switch (result) {
                case INNOCENTS -> participant.role() != Role.MURDERER;
                case MURDERERS -> participant.role() == Role.MURDERER;
                default -> false;
            };
            if (won) {
                amount += switch (participant.role()) {
                    case MURDERER -> coins("murderer-win");
                    case DETECTIVE -> coins("detective-win");
                    default -> coins("innocent-win");
                };
                if (participant.alive() && aliveInnocents == 1 && participant.role() != Role.MURDERER) {
                    amount += coins("hero");
                }
            }
            give(player, amount, plugin.msg().rawString("summary-header"));
            payVault(player, amount);
        }
    }

    private void give(Player player, int amount, String reason) {
        if (player == null || amount <= 0) {
            return;
        }
        PlayerStats stats = plugin.stats().get(player.getUniqueId());
        stats.addCoins(amount);
        plugin.msg().send(player, "coins-earned", Ph.of("amount", amount, "reason", reason));
        checkLevel(player, stats);
    }

    private void checkLevel(Player player, PlayerStats stats) {
        int base = plugin.settings().getInt(null, "rewards.level.base", 100);
        int growth = plugin.settings().getInt(null, "rewards.level.growth", 25);
        int level = 0;
        int needed = base;
        int spent = 0;
        while (stats.coins() >= spent + needed) {
            spent += needed;
            level++;
            needed = base + growth * level;
        }
        if (level > stats.level()) {
            stats.level(level);
            plugin.msg().send(player, "levelup", Ph.of("level", level));
            plugin.sounds().play(player, "levelup");
            if (plugin.settings().getBoolean(null, "rewards.level.broadcast-on-levelup", true)) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.msg().send(online, "levelup-broadcast",
                            Ph.of("player", player.getName(), "level", level));
                }
            }
        }
    }

    private void payVault(Player player, int amount) {
        if (player == null || amount <= 0
                || !plugin.settings().getBoolean(null, "rewards.vault.enabled", false)
                || !plugin.hooks().isEnabled("Vault")) {
            return;
        }
        double multiplier = plugin.settings().getDouble(null, "rewards.vault.multiplier", 1.0);
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var registration = Bukkit.getServicesManager().getRegistration(economyClass.asSubclass(Object.class));
            if (registration == null) {
                return;
            }
            Object economy = registration.getProvider();
            economyClass.getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount * multiplier);
        } catch (Throwable throwable) {
            plugin.getLogger().fine("Vault payout failed: " + throwable.getMessage());
        }
    }
}
