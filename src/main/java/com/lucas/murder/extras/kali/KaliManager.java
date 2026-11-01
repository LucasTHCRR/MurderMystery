package com.lucas.murder.extras.kali;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.game.Arena;
import com.lucas.murder.game.GamePlayer;
import com.lucas.murder.game.Role;
import com.lucas.murder.util.Ph;
import com.lucas.murder.util.Registries;
import com.lucas.murder.util.Tags;
import com.lucas.murder.util.Text;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class KaliManager {

    private final MurderPlugin plugin;
    private final Random random = new Random();

    private final Map<UUID, Integer> favor = new HashMap<>();
    private final Map<UUID, Integer> goldGiven = new HashMap<>();
    private final Map<UUID, Integer> prays = new HashMap<>();
    private final Map<UUID, List<KaliEffect>> active = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> scrambled = new HashSet<>();
    private final Set<UUID> infiniteBow = new HashSet<>();
    private final Map<UUID, Location> stillSince = new HashMap<>();
    private final Map<UUID, Long> lastPraise = new HashMap<>();
    private final List<Spider> spiders = new ArrayList<>();

    public KaliManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection root() {
        return plugin.configs().kali().getConfigurationSection("kali");
    }

    public boolean enabled() {
        ConfigurationSection root = root();
        return plugin.settings().feature("kali") && root != null && root.getBoolean("enabled", true);
    }

    public Material altarBlock() {
        ConfigurationSection root = root();
        Material material = Registries.material(root == null ? null : root.getString("altar-block"));
        return material == null ? Material.END_PORTAL_FRAME : material;
    }

    public void startRound(Arena arena) {
        for (GamePlayer participant : arena.participants()) {
            UUID uuid = participant.uuid();
            favor.put(uuid, root() == null ? 0 : root().getInt("start-favor", 0));
            goldGiven.put(uuid, 0);
            prays.put(uuid, 0);
            active.remove(uuid);
        }
    }

    public void tick(Arena arena) {
        if (!enabled()) {
            return;
        }
        drawAltarParticles(arena);
        for (GamePlayer participant : arena.participants()) {
            Player player = participant.player();
            if (player == null) {
                continue;
            }
            List<KaliEffect> effects = active.get(participant.uuid());
            if (effects == null || effects.isEmpty()) {
                continue;
            }
            for (KaliEffect effect : new ArrayList<>(effects)) {
                tickEffect(arena, participant, player, effect);
                if (effect.tick()) {
                    expire(participant, player, effect);
                    effects.remove(effect);
                }
            }
        }
    }

    private void tickEffect(Arena arena, GamePlayer participant, Player player, KaliEffect effect) {
        switch (effect.type()) {
            case "DOOM" -> {
                int left = effect.secondsLeft();
                if (left <= 10 && left > 0) {
                    player.sendActionBar(plugin.msg().get(player, "kali-doom-warning", Ph.of("seconds", left)));
                }
                if (left == 1) {
                    arena.kill(participant, null, "death-generic");
                }
            }
            case "PRAISE_OR_DIE" -> {
                int interval = effect.config().getInt("interval", 20);
                String phrase = effect.config().getString("phrase", "Praise Kali");
                long last = lastPraise.getOrDefault(participant.uuid(), System.currentTimeMillis());
                int left = interval - (int) ((System.currentTimeMillis() - last) / 1000L);
                if (left <= 0) {
                    arena.kill(participant, null, "death-generic");
                } else if (left <= 5) {
                    player.sendActionBar(plugin.msg().get(player, "kali-praise-warning",
                            Ph.of("phrase", phrase, "seconds", left)));
                }
            }
            case "CHAT_SPAM" -> {
                int intervalTicks = Math.max(20, effect.config().getInt("interval-ticks", 20));
                if (effect.counter() % Math.max(1, intervalTicks / 20) == 0) {
                    List<String> lines = effect.config().getStringList("lines");
                    if (!lines.isEmpty()) {
                        player.sendMessage(Text.of(lines.get(random.nextInt(lines.size()))));
                    }
                }
            }
            case "DAMAGE_WHEN_STILL" -> {
                Location last = stillSince.get(participant.uuid());
                Location now = player.getLocation();
                if (last != null && last.getWorld() == now.getWorld() && last.distanceSquared(now) < 0.5) {
                    double damage = effect.config().getDouble("damage", 1.0);
                    if (player.getHealth() - damage <= 0) {
                        arena.kill(participant, null, "death-generic");
                    } else {
                        player.setHealth(player.getHealth() - damage);
                    }
                } else {
                    stillSince.put(participant.uuid(), now.clone());
                }
            }
            default -> {
            }
        }
    }

    private void drawAltarParticles(Arena arena) {
        ConfigurationSection particles = root() == null ? null : root().getConfigurationSection("particles");
        if (particles == null || !particles.getBoolean("enabled", true) || arena.altar() == null) {
            return;
        }
        double radius = particles.getDouble("radius", 3);
        ConfigurationSection levels = particles.getConfigurationSection("levels");
        if (levels == null) {
            return;
        }
        for (GamePlayer participant : arena.participants()) {
            Player player = participant.player();
            if (player == null || arena.altar().getWorld() == null) {
                continue;
            }
            if (player.getLocation().distanceSquared(arena.altar()) > radius * radius * 9) {
                continue;
            }
            int own = favor(participant.uuid());
            ConfigurationSection chosen = null;
            for (String key : levels.getKeys(false)) {
                ConfigurationSection level = levels.getConfigurationSection(key);
                if (level != null && own <= level.getInt("max-favor", 99)) {
                    chosen = level;
                    break;
                }
            }
            if (chosen == null) {
                continue;
            }
            Particle particle = Registries.particle(chosen.getString("particle", "FLAME"));
            if (particle == null) {
                continue;
            }
            Location center = arena.altar().clone().add(0.5, 1, 0.5);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                Location point = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                if (particle == Particle.DUST) {
                    Color color = Registries.color(chosen.getString("color", "#FFFFFF"));
                    player.spawnParticle(Particle.DUST, point, 1,
                            new Particle.DustOptions(color == null ? Color.WHITE : color, 1F));
                } else {
                    player.spawnParticle(particle, point, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public void click(Arena arena, Player player, ItemStack hand) {
        if (!enabled()) {
            return;
        }
        GamePlayer participant = arena.participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return;
        }
        List<String> allowed = root().getStringList("allow-roles");
        if (!allowed.isEmpty() && participant.role() != null
                && !allowed.contains(participant.role().name())) {
            plugin.msg().send(player, "kali-role-denied");
            return;
        }
        sacrifice(arena, participant, player);
    }

    private void sacrifice(Arena arena, GamePlayer participant, Player player) {
        UUID uuid = participant.uuid();
        int cost = root().getInt("gold-cost", 1);
        int cooldown = root().getInt("cooldown-seconds", 5);
        if (participant.gold() < cost) {
            plugin.msg().send(player, "kali-need-gold", Ph.of("cost", cost));
            return;
        }
        if (!plugin.cooldowns().tryUse(uuid, "kali", cooldown * 1000L)) {
            plugin.msg().send(player, "kali-pray-cooldown",
                    Ph.of("seconds", plugin.cooldowns().remainingSeconds(uuid, "kali")));
            return;
        }
        int limit = root().getInt("max-prays-per-round", 0);
        if (limit > 0 && prays.getOrDefault(uuid, 0) >= limit) {
            plugin.msg().send(player, "kali-pray-limit");
            return;
        }

        participant.gold(participant.gold() - cost);
        arena.gold().syncItem(participant, player);
        prays.merge(uuid, 1, Integer::sum);
        plugin.msg().send(player, "kali-praying");
        plugin.sounds().play(player, "kali-pray");

        boolean blessing = random.nextDouble() * 100 < root().getDouble("blessing-chance", 50);
        ConfigurationSection suspense = root().getConfigurationSection("suspense");
        int delay = suspense != null && suspense.getBoolean("enabled", true)
                ? suspense.getInt("ticks", 40) : 0;
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> roll(arena, participant, player, blessing), Math.max(1, delay));
    }

    public double blessingChance(int own) {
        ConfigurationSection curve = root().getConfigurationSection("favor-curve");
        if (curve == null) {
            return 50;
        }
        int lowerKey = 0;
        double lowerValue = 0;
        int upperKey = Integer.MAX_VALUE;
        double upperValue = 100;
        for (String key : curve.getKeys(false)) {
            int point;
            try {
                point = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            double value = curve.getDouble(key);
            if (point <= own && point >= lowerKey) {
                lowerKey = point;
                lowerValue = value;
            }
            if (point >= own && point <= upperKey) {
                upperKey = point;
                upperValue = value;
            }
        }
        if (upperKey == Integer.MAX_VALUE || upperKey == lowerKey) {
            return lowerValue;
        }
        double ratio = (double) (own - lowerKey) / (upperKey - lowerKey);
        return lowerValue + (upperValue - lowerValue) * ratio;
    }

    private void roll(Arena arena, GamePlayer participant, Player player, boolean blessing) {
        ConfigurationSection table = root().getConfigurationSection(blessing ? "blessings" : "curses");
        if (table == null) {
            return;
        }
        List<String> ids = new ArrayList<>();
        double sum = 0;
        for (String key : table.getKeys(false)) {
            ConfigurationSection entry = table.getConfigurationSection(key);
            if (entry == null || !entry.getBoolean("enabled", true)) {
                continue;
            }
            List<String> deny = entry.getStringList("deny-roles");
            if (participant.role() != null && deny.contains(participant.role().name())) {
                continue;
            }
            ids.add(key);
            sum += entry.getDouble("chance", 0);
        }
        if (ids.isEmpty() || sum <= 0) {
            return;
        }
        double roll = random.nextDouble() * sum;
        String chosen = ids.get(ids.size() - 1);
        double walked = 0;
        for (String id : ids) {
            walked += table.getConfigurationSection(id).getDouble("chance", 0);
            if (roll <= walked) {
                chosen = id;
                break;
            }
        }
        apply(arena, participant, player, table.getConfigurationSection(chosen), chosen, blessing);
    }

    public void apply(Arena arena, GamePlayer participant, Player player,
                      ConfigurationSection config, String id, boolean blessing) {
        if (config == null) {
            return;
        }
        String type = config.getString("type", id.toUpperCase(Locale.ROOT));
        String name = config.getString("name", id);
        int duration = config.getInt("duration", config.getInt("seconds", 0));

        plugin.msg().send(player, blessing ? "kali-blessed" : "kali-cursed", Ph.of("name", Text.plain(name)));
        String message = config.getString("message");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(Text.of(message.replace("%prefix%", plugin.msg().prefix())
                    .replace("%player%", detectiveName(arena))));
        }
        plugin.sounds().play(player, blessing ? "kali-blessing" : "kali-curse");
        announce(arena, player, blessing);

        KaliEffect effect = new KaliEffect(id, type, name, blessing, config, duration);
        active.computeIfAbsent(participant.uuid(), key -> new ArrayList<>()).add(effect);

        switch (type) {
            case "REVEAL_DETECTIVE" -> {
                String detective = detectiveName(arena);
                plugin.msg().send(player, detective == null ? "kali-no-detective" : "kali-reveal-detective",
                        Ph.of("player", detective == null ? "-" : detective));
            }
            case "GOLD_MULTIPLIER" -> participant.goldMultiplier(config.getInt("multiplier", 3));
            case "SINGLE_SHOT" -> {
                arena.giveBow(participant, false);
                int arrows = config.getInt("arrows", 1);
                ItemStack arrow = plugin.items().arrow();
                arrow.setAmount(arrows);
                player.getInventory().setItem(plugin.items().slot("arrow", 9), arrow);
            }
            case "ANCIENT_BOW" -> {
                arena.giveBow(participant, false);
                infiniteBow.add(participant.uuid());
                ItemStack arrow = plugin.items().arrow();
                arrow.setAmount(64);
                player.getInventory().setItem(plugin.items().slot("arrow", 9), arrow);
            }
            case "EXTRA_LIFE" -> participant.extraLives(participant.extraLives() + config.getInt("lives", 1));
            case "CLEANSE" -> cleanse(participant, player);
            case "SAFE_TELEPORT" -> {
                if (!arena.spawns().isEmpty()) {
                    player.teleport(arena.spawns().get(random.nextInt(arena.spawns().size())));
                }
            }
            case "SHOW_PLAYERS" -> {
                for (Player other : arena.onlinePlayers()) {
                    other.setGlowing(true);
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Player other : arena.onlinePlayers()) {
                        other.setGlowing(false);
                    }
                }, Math.max(20L, duration * 20L));
            }
            case "SPEED" -> addPotion(player, "SPEED", config.getInt("amplifier", 1), duration);
            case "MUTE" -> plugin.chat().mute(participant.uuid());
            case "PRAISE_OR_DIE" -> lastPraise.put(participant.uuid(), System.currentTimeMillis());
            case "SCRAMBLE_SCOREBOARD" -> scrambled.add(participant.uuid());
            case "FILL_INVENTORY" -> fillInventory(player, config);
            case "SPAWN_SPIDERS" -> spawnSpiders(arena, player, config);
            case "FAKE_SWORD" -> player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            case "SLOWNESS" -> addPotion(player, "SLOWNESS", config.getInt("amplifier", 1), duration);
            case "BLIND" -> addPotion(player, "BLINDNESS", 0, duration);
            case "NO_GOLD" -> participant.goldBlocked(true);
            case "FREEZE" -> freeze(arena, participant, player, config);
            case "REVEAL_SELF" -> player.setGlowing(true);
            case "DOOM", "CHAT_SPAM", "DAMAGE_WHEN_STILL" -> {

            }
            default -> {
            }
        }
    }

    private void expire(GamePlayer participant, Player player, KaliEffect effect) {
        switch (effect.type()) {
            case "GOLD_MULTIPLIER" -> participant.goldMultiplier(1);
            case "NO_GOLD" -> participant.goldBlocked(false);
            case "MUTE" -> plugin.chat().unmute(participant.uuid());
            case "SCRAMBLE_SCOREBOARD" -> scrambled.remove(participant.uuid());
            case "FREEZE" -> frozen.remove(participant.uuid());
            case "ANCIENT_BOW" -> infiniteBow.remove(participant.uuid());
            case "FILL_INVENTORY" -> restoreInventory(player);
            case "REVEAL_SELF" -> player.setGlowing(false);
            default -> {
            }
        }
        plugin.msg().send(player, "kali-curse-expired", Ph.of("name", Text.plain(effect.name())));
    }

    private void cleanse(GamePlayer participant, Player player) {
        List<KaliEffect> effects = active.get(participant.uuid());
        if (effects == null) {
            return;
        }
        int removed = 0;
        for (KaliEffect effect : new ArrayList<>(effects)) {
            if (effect.blessing()) {
                continue;
            }
            if ("DOOM".equals(effect.type()) && !effect.config().getBoolean("cleansable", true)) {
                continue;
            }
            expire(participant, player, effect);
            effects.remove(effect);
            removed++;
        }
        plugin.msg().send(player, "kali-cleansed", Ph.of("amount", removed));
    }

    private void freeze(Arena arena, GamePlayer participant, Player player, ConfigurationSection config) {

        // Nie über einer Todeszone einfrieren, das wäre ein Tod ohne Chance
        if (config.getBoolean("safe-only", true)) {
            for (var zone : arena.zones().values()) {
                if (zone.type() == com.lucas.murder.region.ZoneType.INSTANT
                        && zone.area().contains(player.getLocation())) {
                    return;
                }
            }
        }
        frozen.add(participant.uuid());
    }

    private void fillInventory(Player player, ConfigurationSection config) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        Material fill = Registries.material(config.getString("fill-material", "COBWEB"));
        if (fill == null) {
            fill = Material.COBWEB;
        }
        List<String> keep = config.getStringList("keep");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack != null && keep.contains(stack.getType().name())) {
                continue;
            }
            player.getInventory().setItem(slot, new ItemStack(fill));
        }
    }

    private void restoreInventory(Player player) {
        ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved);
        }
    }

    private void spawnSpiders(Arena arena, Player player, ConfigurationSection config) {
        int amount = config.getInt("amount", 3);
        for (int i = 0; i < amount; i++) {
            Location location = player.getLocation().clone().add(
                    random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (location.getWorld() == null) {
                continue;
            }
            Spider spider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
            spider.setTarget(player);
            Tags.tag(spider, arena.name(), Tags.KIND_SPIDER);
            spiders.add(spider);
        }
    }

    private void addPotion(Player player, String effectName, int amplifier, int duration) {
        PotionEffectType type = Registries.effect(effectName);
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type,
                    duration <= 0 ? Integer.MAX_VALUE : duration * 20, amplifier, false, false));
        }
    }

    private void announce(Arena arena, Player player, boolean blessing) {
        String mode = root().getString("announce", "SELF").toUpperCase(Locale.ROOT);
        if ("SELF".equals(mode) || "NONE".equals(mode)) {
            return;
        }
        String key = blessing ? "kali-blessed-nearby" : "kali-cursed-nearby";
        double radius = root().getInt("announce-radius", 8);
        for (Player other : arena.onlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if ("NEARBY".equals(mode)
                    && other.getLocation().distanceSquared(player.getLocation()) > radius * radius) {
                continue;
            }
            plugin.msg().send(other, key, Ph.of("player", player.getName()));
        }
    }

    private String detectiveName(Arena arena) {
        List<GamePlayer> detectives = arena.aliveOf(Role.DETECTIVE);
        return detectives.isEmpty() ? null : detectives.get(0).name();
    }

    public int favor(UUID uuid) {
        return favor.getOrDefault(uuid, 0);
    }

    public void favor(UUID uuid, int value) {
        favor.put(uuid, value);
    }

    public int goldGiven(UUID uuid) {
        return goldGiven.getOrDefault(uuid, 0);
    }

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public boolean isScrambled(UUID uuid) {
        return scrambled.contains(uuid);
    }

    public boolean hasInfiniteBow(UUID uuid) {
        return infiniteBow.contains(uuid);
    }

    public boolean handlePraise(UUID uuid, String message) {
        List<KaliEffect> effects = active.get(uuid);
        if (effects == null) {
            return false;
        }
        for (KaliEffect effect : effects) {
            if (!"PRAISE_OR_DIE".equals(effect.type())) {
                continue;
            }
            String phrase = effect.config().getString("phrase", "Praise Kali");
            if (message.trim().equalsIgnoreCase(phrase)) {
                lastPraise.put(uuid, System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    public boolean blocksPickup(UUID uuid, Material material) {
        List<KaliEffect> effects = active.get(uuid);
        if (effects == null) {
            return false;
        }
        for (KaliEffect effect : effects) {
            if ("FILL_INVENTORY".equals(effect.type())
                    && effect.config().getStringList("block-pickup").contains(material.name())) {
                return true;
            }
        }
        return false;
    }

    public List<KaliEffect> effects(UUID uuid) {
        return active.getOrDefault(uuid, List.of());
    }

    public void clearPlayer(UUID uuid, Player player) {
        active.remove(uuid);
        frozen.remove(uuid);
        scrambled.remove(uuid);
        infiniteBow.remove(uuid);
        stillSince.remove(uuid);
        lastPraise.remove(uuid);
        plugin.chat().unmute(uuid);
        if (player != null) {
            restoreInventory(player);
            player.setGlowing(false);
        }
        savedInventories.remove(uuid);
    }

    public void clearArena(Arena arena) {
        for (GamePlayer participant : arena.participants()) {
            clearPlayer(participant.uuid(), participant.player());
            favor.remove(participant.uuid());
            goldGiven.remove(participant.uuid());
            prays.remove(participant.uuid());
        }
        for (Spider spider : new ArrayList<>(spiders)) {
            if (spider.isValid()) {
                spider.remove();
            }
        }
        spiders.clear();
    }

    public void clearAll() {
        favor.clear();
        goldGiven.clear();
        prays.clear();
        active.clear();
        frozen.clear();
        scrambled.clear();
        infiniteBow.clear();
        stillSince.clear();
        lastPraise.clear();
        savedInventories.clear();
        for (Spider spider : new ArrayList<>(spiders)) {
            if (spider.isValid()) {
                spider.remove();
            }
        }
        spiders.clear();
    }

    public Map<String, Double> chanceSums() {
        Map<String, Double> sums = new HashMap<>();
        for (String table : List.of("blessings", "curses")) {
            ConfigurationSection section = root() == null ? null : root().getConfigurationSection(table);
            double sum = 0;
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection entry = section.getConfigurationSection(key);
                    if (entry != null && entry.getBoolean("enabled", true)) {
                        sum += entry.getDouble("chance", 0);
                    }
                }
            }
            sums.put(table, sum);
        }
        return sums;
    }
}
