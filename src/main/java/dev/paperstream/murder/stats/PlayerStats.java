package dev.paperstream.murder.stats;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerStats {

    private final UUID uuid;
    private String name;
    private int games;
    private int wins;
    private int losses;
    private int kills;
    private int murdererWins;
    private int detectiveWins;
    private int innocentWins;
    private int goldCollected;
    private int bowsEarned;
    private int heroes;
    private long playtimeSeconds;
    private int coins;
    private int level;
    private boolean scoreboardHidden;
    private final List<String> owned = new ArrayList<>();
    private final Map<String, String> equipped = new HashMap<>();
    private boolean dirty;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static PlayerStats load(UUID uuid, ConfigurationSection section) {
        PlayerStats stats = new PlayerStats(uuid, section.getString("name", "?"));
        stats.games = section.getInt("games");
        stats.wins = section.getInt("wins");
        stats.losses = section.getInt("losses");
        stats.kills = section.getInt("kills");
        stats.murdererWins = section.getInt("murderer-wins");
        stats.detectiveWins = section.getInt("detective-wins");
        stats.innocentWins = section.getInt("innocent-wins");
        stats.goldCollected = section.getInt("gold-collected");
        stats.bowsEarned = section.getInt("bows-earned");
        stats.heroes = section.getInt("heroes");
        stats.playtimeSeconds = section.getLong("playtime-seconds");
        stats.coins = section.getInt("coins");
        stats.level = section.getInt("level");
        stats.scoreboardHidden = section.getBoolean("scoreboard-hidden", false);
        stats.owned.addAll(section.getStringList("cosmetics.owned"));
        ConfigurationSection equippedSection = section.getConfigurationSection("cosmetics.equipped");
        if (equippedSection != null) {
            for (String key : equippedSection.getKeys(false)) {
                stats.equipped.put(key, equippedSection.getString(key));
            }
        }
        return stats;
    }

    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("games", games);
        section.set("wins", wins);
        section.set("losses", losses);
        section.set("kills", kills);
        section.set("murderer-wins", murdererWins);
        section.set("detective-wins", detectiveWins);
        section.set("innocent-wins", innocentWins);
        section.set("gold-collected", goldCollected);
        section.set("bows-earned", bowsEarned);
        section.set("heroes", heroes);
        section.set("playtime-seconds", playtimeSeconds);
        section.set("coins", coins);
        section.set("level", level);
        section.set("scoreboard-hidden", scoreboardHidden);
        section.set("cosmetics.owned", owned);
        section.set("cosmetics.equipped", null);
        for (Map.Entry<String, String> entry : equipped.entrySet()) {
            section.set("cosmetics.equipped." + entry.getKey(), entry.getValue());
        }
        dirty = false;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    private void touch() {
        dirty = true;
    }

    public int games() {
        return games;
    }

    public void addGame() {
        games++;
        touch();
    }

    public int wins() {
        return wins;
    }

    public void addWin() {
        wins++;
        touch();
    }

    public int losses() {
        return losses;
    }

    public void addLoss() {
        losses++;
        touch();
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
        touch();
    }

    public void addRoleWin(String role) {
        switch (role) {
            case "MURDERER" -> murdererWins++;
            case "DETECTIVE" -> detectiveWins++;
            default -> innocentWins++;
        }
        touch();
    }

    public int goldCollected() {
        return goldCollected;
    }

    public void addGold(int amount) {
        goldCollected += amount;
        touch();
    }

    public int bowsEarned() {
        return bowsEarned;
    }

    public void addBow() {
        bowsEarned++;
        touch();
    }

    public int heroes() {
        return heroes;
    }

    public void addHero() {
        heroes++;
        touch();
    }

    public long playtimeSeconds() {
        return playtimeSeconds;
    }

    public void addPlaytime(long seconds) {
        playtimeSeconds += seconds;
        touch();
    }

    public int coins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins += amount;
        touch();
    }

    public boolean spendCoins(int amount) {
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        touch();
        return true;
    }

    public int level() {
        return level;
    }

    public void level(int level) {
        this.level = level;
        touch();
    }

    public boolean scoreboardHidden() {
        return scoreboardHidden;
    }

    public void scoreboardHidden(boolean hidden) {
        this.scoreboardHidden = hidden;
        touch();
    }

    public List<String> owned() {
        return owned;
    }

    public boolean owns(String id) {
        return owned.contains(id);
    }

    public void addOwned(String id) {
        if (!owned.contains(id)) {
            owned.add(id);
            touch();
        }
    }

    public Map<String, String> equipped() {
        return equipped;
    }

    public String equipped(String category) {
        return equipped.get(category);
    }

    public void equip(String category, String id) {
        equipped.put(category, id);
        touch();
    }

    public String formattedKd() {
        int deaths = Math.max(1, games - wins);
        return String.format(java.util.Locale.ROOT, "%.2f", (double) kills / deaths);
    }

    public int statValue(String stat) {
        return switch (stat.toLowerCase(java.util.Locale.ROOT)) {
            case "kills" -> kills;
            case "games" -> games;
            case "coins" -> coins;
            case "level" -> level;
            case "gold" -> goldCollected;
            case "heroes" -> heroes;
            default -> wins;
        };
    }
}
