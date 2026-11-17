package dev.paperstream.murder.game;

import dev.paperstream.murder.util.PlayerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class GamePlayer {

    private final UUID uuid;
    private final String name;
    private final PlayerSnapshot snapshot;

    private Role role;
    private boolean alive = true;
    private boolean spectator;
    private int gold;
    private int goldCollected;
    private int kills;
    private boolean hasBow;
    private boolean hero;
    private int extraLives;
    private int goldMultiplier = 1;
    private boolean goldBlocked;
    private int aliasNumber;
    private Location deathLocation;
    private Location lastSafeLocation;
    private long outsideSince;
    private long lastMove = System.currentTimeMillis();
    private long joinedAt = System.currentTimeMillis();
    private boolean scoreboardHidden;
    private String votedFor;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.snapshot = PlayerSnapshot.take(player);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Player player() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean online() {
        Player player = player();
        return player != null && player.isOnline();
    }

    public PlayerSnapshot snapshot() {
        return snapshot;
    }

    public Role role() {
        return role;
    }

    public void role(Role role) {
        this.role = role;
    }

    public boolean alive() {
        return alive;
    }

    public void alive(boolean alive) {
        this.alive = alive;
    }

    public boolean spectator() {
        return spectator;
    }

    public void spectator(boolean spectator) {
        this.spectator = spectator;
    }

    public int gold() {
        return gold;
    }

    public int addGold(int amount) {
        int added = amount * Math.max(1, goldMultiplier);
        gold += added;
        goldCollected += added;
        return gold;
    }

    public int goldCollected() {
        return goldCollected;
    }

    public void gold(int gold) {
        this.gold = gold;
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public boolean hasBow() {
        return hasBow;
    }

    public void hasBow(boolean hasBow) {
        this.hasBow = hasBow;
    }

    public boolean hero() {
        return hero;
    }

    public void hero(boolean hero) {
        this.hero = hero;
    }

    public int extraLives() {
        return extraLives;
    }

    public void extraLives(int extraLives) {
        this.extraLives = extraLives;
    }

    public boolean consumeExtraLife() {
        if (extraLives > 0) {
            extraLives--;
            return true;
        }
        return false;
    }

    public int goldMultiplier() {
        return goldMultiplier;
    }

    public void goldMultiplier(int goldMultiplier) {
        this.goldMultiplier = Math.max(1, goldMultiplier);
    }

    public boolean goldBlocked() {
        return goldBlocked;
    }

    public void goldBlocked(boolean goldBlocked) {
        this.goldBlocked = goldBlocked;
    }

    public int aliasNumber() {
        return aliasNumber;
    }

    public void aliasNumber(int aliasNumber) {
        this.aliasNumber = aliasNumber;
    }

    public Location deathLocation() {
        return deathLocation;
    }

    public void deathLocation(Location deathLocation) {
        this.deathLocation = deathLocation;
    }

    public Location lastSafeLocation() {
        return lastSafeLocation;
    }

    public void lastSafeLocation(Location lastSafeLocation) {
        this.lastSafeLocation = lastSafeLocation;
    }

    public long outsideSince() {
        return outsideSince;
    }

    public void outsideSince(long outsideSince) {
        this.outsideSince = outsideSince;
    }

    public long lastMove() {
        return lastMove;
    }

    public void touchMove() {
        this.lastMove = System.currentTimeMillis();
    }

    public long joinedAt() {
        return joinedAt;
    }

    public void resetJoinTime() {
        this.joinedAt = System.currentTimeMillis();
    }

    public boolean scoreboardHidden() {
        return scoreboardHidden;
    }

    public void scoreboardHidden(boolean scoreboardHidden) {
        this.scoreboardHidden = scoreboardHidden;
    }

    public String votedFor() {
        return votedFor;
    }

    public void votedFor(String votedFor) {
        this.votedFor = votedFor;
    }
}
