package com.lucas.murder.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PlayerSnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armour;
    private final ItemStack offHand;
    private final GameMode gameMode;
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final boolean allowFlight;
    private final boolean flying;
    private final float walkSpeed;
    private final float flySpeed;
    private final int fireTicks;
    private final List<PotionEffect> effects;

    private PlayerSnapshot(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armour = player.getInventory().getArmorContents().clone();
        this.offHand = player.getInventory().getItemInOffHand().clone();
        this.gameMode = player.getGameMode();
        this.location = player.getLocation().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.walkSpeed = player.getWalkSpeed();
        this.flySpeed = player.getFlySpeed();
        this.fireTicks = player.getFireTicks();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
    }

    public static PlayerSnapshot take(Player player) {
        return new PlayerSnapshot(player);
    }

    public static void wipe(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setItemOnCursor(null);
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setVelocity(player.getVelocity().zero());
        player.setArrowsInBody(0);
        player.setFallDistance(0F);
        player.setExp(0F);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGlowing(false);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        double max = maxHealth(player);
        player.setHealth(max);
        player.resetTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
    }

    public void restore(Player player) {
        wipe(player);
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armour);
        player.getInventory().setItemInOffHand(offHand);
        player.setGameMode(gameMode);
        player.setHealth(Math.min(health, maxHealth(player)));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.setWalkSpeed(walkSpeed);
        player.setFlySpeed(flySpeed);
        player.setFireTicks(fireTicks);
        player.addPotionEffects(effects);
        player.updateInventory();
    }

    public Location location() {
        return location.clone();
    }

    public Collection<PotionEffect> effects() {
        return effects;
    }

    @SuppressWarnings("deprecation")
    private static double maxHealth(Player player) {
        return player.getMaxHealth();
    }
}
