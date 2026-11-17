package dev.paperstream.murder.game;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.config.Overridable;
import dev.paperstream.murder.region.Cuboid;
import dev.paperstream.murder.region.Zone;
import dev.paperstream.murder.util.Locations;
import dev.paperstream.murder.util.Ph;
import dev.paperstream.murder.util.PlayerSnapshot;
import dev.paperstream.murder.util.Tags;
import dev.paperstream.murder.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Arena implements Overridable {

    private final MurderPlugin plugin;
    private final String name;

    private String displayName;
    private String author = "";
    private Material icon = Material.PAPER;
    private boolean enabled;
    private Integer minPlayers;
    private Integer maxPlayers;
    private Integer roundTime;
    private Integer countdownTime;
    private int forcedMurderers = 0;
    private int forcedDetectives = -1;

    private final List<Location> spawns = new ArrayList<>();
    private final List<Location> goldSpawns = new ArrayList<>();
    private final List<Location> potionStations = new ArrayList<>();
    private Location spectatorSpawn;
    private Location lobby;
    private Location altar;
    private Cuboid bounds;
    private final Map<String, Zone> zones = new LinkedHashMap<>();
    private final Map<String, dev.paperstream.murder.region.Trap> traps = new LinkedHashMap<>();
    private ConfigurationSection overrides;

    private GameState state = GameState.DISABLED;
    private final Map<UUID, GamePlayer> participants = new LinkedHashMap<>();
    private final GoldManager gold;
    private final ArenaMap map;
    private BukkitTask ticker;
    private int tickCounter;
    private int countdown;
    private int timeLeft;
    private int endingLeft;
    private long roundStart;
    private WinChecker.Result result = WinChecker.Result.NONE;
    private String lastMurdererName = "-";
    private String lastDetectiveName = "-";
    private String replayId;
    private int spawnRotation;
    private boolean weaponsGiven;
    private boolean revealed;

    public Arena(MurderPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.displayName = name;
        this.gold = new GoldManager(plugin, this);
        this.map = new ArenaMap(plugin, this);
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName == null || displayName.isEmpty() ? name : displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public String author() {
        return author == null ? "" : author;
    }

    public void author(String author) {
        this.author = author;
    }

    public Material icon() {
        return icon == null ? Material.PAPER : icon;
    }

    public void icon(Material icon) {
        this.icon = icon;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            state = GameState.DISABLED;
        } else if (state == GameState.DISABLED) {
            state = GameState.WAITING;
        }
    }

    public GameState state() {
        return state;
    }

    public void state(GameState state) {
        this.state = state;
        plugin.signs().update(this);
    }

    @Override
    public ConfigurationSection overrides() {
        return overrides;
    }

    public void overrides(ConfigurationSection overrides) {
        this.overrides = overrides;
    }

    public List<Location> spawns() {
        return spawns;
    }

    public List<Location> goldSpawns() {
        return goldSpawns;
    }

    public List<Location> potionStations() {
        return potionStations;
    }

    public Location spectatorSpawn() {
        return spectatorSpawn;
    }

    public void spectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public Location lobby() {
        return lobby;
    }

    public void lobby(Location lobby) {
        this.lobby = lobby;
    }

    public Location waitingLobby() {
        return lobby != null ? lobby : plugin.arenas().globalLobby();
    }

    public Location altar() {
        return altar;
    }

    public void altar(Location altar) {
        this.altar = altar;
    }

    public Cuboid bounds() {
        return bounds;
    }

    public void bounds(Cuboid bounds) {
        this.bounds = bounds;
    }

    public Map<String, Zone> zones() {
        return zones;
    }

    public Map<String, dev.paperstream.murder.region.Trap> traps() {
        return traps;
    }

    public GoldManager gold() {
        return gold;
    }

    public int minPlayers() {
        return minPlayers != null ? minPlayers : plugin.settings().getInt(this, "min-players", 4);
    }

    public void minPlayers(Integer minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int maxPlayers() {
        return maxPlayers != null ? maxPlayers : plugin.settings().getInt(this, "max-players", 16);
    }

    public void maxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int roundTime() {
        return roundTime != null ? roundTime : plugin.settings().getInt(this, "round-time", 480);
    }

    public void roundTime(Integer roundTime) {
        this.roundTime = roundTime;
    }

    public int countdownTime() {
        return countdownTime != null ? countdownTime : plugin.settings().getInt(this, "lobby-countdown", 30);
    }

    public void countdownTime(Integer countdownTime) {
        this.countdownTime = countdownTime;
    }

    public int forcedMurderers() {
        return forcedMurderers;
    }

    public void forcedMurderers(int forcedMurderers) {
        this.forcedMurderers = forcedMurderers;
    }

    public int forcedDetectives() {
        return forcedDetectives;
    }

    public void forcedDetectives(int forcedDetectives) {
        this.forcedDetectives = forcedDetectives;
    }

    public int countdown() {
        return countdown;
    }

    public int timeLeft() {
        return timeLeft;
    }

    public int endingLeft() {
        return endingLeft;
    }

    public WinChecker.Result result() {
        return result;
    }

    public String lastMurdererName() {
        return lastMurdererName;
    }

    public String lastDetectiveName() {
        return lastDetectiveName;
    }

    public String replayId() {
        return replayId;
    }

    public boolean revealed() {
        return revealed;
    }

    public long roundStart() {
        return roundStart;
    }

    public Collection<GamePlayer> participants() {
        return participants.values();
    }

    public GamePlayer participant(UUID uuid) {
        return participants.get(uuid);
    }

    public boolean contains(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public int playerCount() {
        int count = 0;
        for (GamePlayer participant : participants.values()) {
            if (!participant.spectator()) {
                count++;
            }
        }
        return count;
    }

    public int aliveCount() {
        int count = 0;
        for (GamePlayer participant : participants.values()) {
            if (!participant.spectator() && participant.alive()) {
                count++;
            }
        }
        return count;
    }

    public List<Player> onlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (GamePlayer participant : participants.values()) {
            Player player = participant.player();
            if (player != null && player.isOnline()) {
                list.add(player);
            }
        }
        return list;
    }

    public List<GamePlayer> aliveOf(Role role) {
        List<GamePlayer> list = new ArrayList<>();
        for (GamePlayer participant : participants.values()) {
            if (participant.alive() && !participant.spectator() && participant.role() == role) {
                list.add(participant);
            }
        }
        return list;
    }

    public void broadcast(String key, Map<String, String> placeholders) {
        for (Player player : onlinePlayers()) {
            plugin.msg().send(player, key, placeholders);
        }
    }

    public void broadcast(String key) {
        broadcast(key, Ph.none());
    }

    public void broadcast(Component component) {
        for (Player player : onlinePlayers()) {
            player.sendMessage(component);
        }
    }

    public void title(String titleKey, String subtitleKey, Map<String, String> placeholders) {
        if (!plugin.settings().feature("titles")) {
            return;
        }
        for (Player player : onlinePlayers()) {
            player.showTitle(Title.title(
                    plugin.msg().get(player, titleKey, placeholders),
                    plugin.msg().get(player, subtitleKey, placeholders),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
    }

    public boolean canJoin(Player player) {
        return enabled && state.joinable()
                && (playerCount() < maxPlayers() || player.hasPermission("murder.join.full"));
    }

    public void join(Player player) {
        GamePlayer participant = new GamePlayer(player);
        participants.put(player.getUniqueId(), participant);

        PlayerSnapshot.wipe(player);
        player.setGameMode(GameMode.ADVENTURE);
        Location lobbyLocation = waitingLobby();
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
        giveLobbyItems(player);

        plugin.msg().send(player, "join-self", Ph.of("arena", displayName()));
        broadcast("join-broadcast", Ph.of(
                "player", player.getName(),
                "current", playerCount(),
                "max", maxPlayers()));
        plugin.sounds().play(onlinePlayers(), "join");
        plugin.boards().apply(player, this);
        plugin.nametags().apply(this, player);
        plugin.signs().update(this);

        if (state == GameState.WAITING && playerCount() >= minPlayers()) {
            startCountdown(false);
        }
        ensureTicker();
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        if (plugin.settings().feature("map-voting")) {
            player.getInventory().setItem(plugin.items().slot("lobby.vote", 4),
                    plugin.items().build("lobby.vote", Material.PAPER));
        }
        if (plugin.settings().feature("cosmetics")) {
            player.getInventory().setItem(plugin.items().slot("lobby.shop", 0),
                    plugin.items().build("lobby.shop", Material.CHEST));
        }
        player.getInventory().setItem(plugin.items().slot("lobby.leave", 8),
                plugin.items().build("lobby.leave", Material.RED_BED));
    }

    public void leave(Player player, boolean silent) {
        GamePlayer participant = participants.remove(player.getUniqueId());
        if (participant == null) {
            return;
        }
        boolean wasAlive = participant.alive() && !participant.spectator();
        Location quitLocation = player.getLocation().clone();
        plugin.cleanup().resetPlayer(this, participant, player);

        if (!silent) {
            plugin.msg().send(player, "leave-self");
            broadcast("leave-broadcast", Ph.of(
                    "player", player.getName(),
                    "current", playerCount(),
                    "max", maxPlayers()));
            plugin.sounds().play(player, "leave");
        }
        plugin.signs().update(this);

        if (silent) {
            return;
        }
        if (state == GameState.COUNTDOWN && playerCount() < minPlayers()) {
            cancelCountdown();
        }
        if (state == GameState.RUNNING && wasAlive) {
            participant.alive(false);
            if (participant.role() == Role.DETECTIVE) {
                dropDetectiveBow(safeBowSpot(quitLocation));
            }
            if (!plugin.settings().getBoolean(this, "nametags.hide-in-death-messages", false)) {
                broadcast("death-quit", Ph.of("player", player.getName()));
            }
            checkWin();
        }
        if (participants.isEmpty() && state != GameState.RUNNING) {
            stopTicker();
        }
    }

    public void addSpectator(Player player) {
        GamePlayer participant = new GamePlayer(player);
        participant.spectator(true);
        participant.alive(false);
        participants.put(player.getUniqueId(), participant);
        makeSpectator(participant, player);
        plugin.msg().send(player, "spectator-joined");
        plugin.boards().apply(player, this);
    }

    private void makeSpectator(GamePlayer participant, Player player) {
        participant.spectator(true);
        participant.alive(false);
        PlayerSnapshot.wipe(player);
        player.setGameMode(GameMode.SPECTATOR);
        if (spectatorSpawn != null) {
            player.teleport(spectatorSpawn);
        }
        if (plugin.settings().getBoolean(this, "spectator.fly", true)) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        player.sendMessage(plugin.msg().get(player, "spectator-controls", Ph.none())
                .clickEvent(ClickEvent.runCommand("/murder leave")));
        plugin.nametags().apply(this, player);
    }

    public void startCountdown(boolean forced) {
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            return;
        }
        state(GameState.COUNTDOWN);
        countdown = forced ? 3 : countdownTime();
        broadcast("countdown-start", Ph.of("seconds", countdown));
        ensureTicker();
    }

    public void cancelCountdown() {
        if (state != GameState.COUNTDOWN) {
            return;
        }
        state(GameState.WAITING);
        countdown = 0;
        broadcast("countdown-cancelled");
    }

    public void start() {
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            return;
        }
        if (spawns.isEmpty()) {
            plugin.getLogger().warning("Arena " + name + " has no spawn points - cannot start");
            return;
        }
        state(GameState.RUNNING);
        weaponsGiven = false;
        revealed = false;
        countdown = 0;
        timeLeft = roundTime();
        roundStart = System.currentTimeMillis();
        // Rollen stehen erst am Ende von start() fest
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager()
                .callEvent(new dev.paperstream.murder.api.event.MurderGameStartEvent(name, onlinePlayers())));
        result = WinChecker.Result.NONE;
        spawnRotation = 0;

        List<GamePlayer> playing = new ArrayList<>();
        for (GamePlayer participant : participants.values()) {
            if (!participant.spectator()) {
                playing.add(participant);
            }
        }
        Collections.shuffle(playing);

        plugin.roles().assign(this);
        assignAliases(playing);
        lastMurdererName = namesOf(playing, Role.MURDERER);
        lastDetectiveName = namesOf(playing, Role.DETECTIVE);

        for (GamePlayer participant : playing) {
            Player player = participant.player();
            if (player == null) {
                continue;
            }
            participant.alive(true);
            participant.resetJoinTime();
            plugin.votes().clear(participant.uuid());
            PlayerSnapshot.wipe(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(nextSpawn());
            participant.lastSafeLocation(player.getLocation());
            player.getInventory().clear();
            ItemStack mapItem = map.item();
            if (mapItem != null) {
                player.getInventory().setItem(plugin.items().slot("map", 7), mapItem);
            }
            announceRole(participant, player);
            plugin.nametags().apply(this, player);
        }

        if (name.equalsIgnoreCase(plugin.votes().forced() == null ? "" : plugin.votes().forced())) {
            plugin.votes().force(null);
        }
        gold.fill();
        plugin.kali().startRound(this);
        plugin.potions().startRound(this);
        plugin.sounds().play(onlinePlayers(), "game-start");
        broadcast("game-start");
        int delay = plugin.settings().getInt(this, "roles.weapon-delay", 15);
        if (delay > 0) {
            broadcast("weapons-soon", Ph.of("seconds", delay));
        } else {
            handOutWeapons();
        }
        broadcast("map-info", Ph.of("map", displayName(), "author", author()));
        plugin.boards().refresh(this);

        if (plugin.settings().feature("replays")
                && plugin.settings().getBoolean(this, "replay.auto-record", true)
                && playerCount() >= plugin.settings().getInt(this, "replay.min-players", 4)) {
            replayId = plugin.replays().startRecording(this);
        }
        ensureTicker();
    }

    private void assignAliases(List<GamePlayer> playing) {
        boolean needed = plugin.settings().getBoolean(this, "nametags.hide-in-chat", false)
                || plugin.settings().getBoolean(this, "nametags.hide-in-tablist", false);
        if (!needed) {
            return;
        }
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= playing.size(); i++) {
            numbers.add(i);
        }
        if (plugin.settings().getBoolean(this, "nametags.shuffle-aliases-each-round", true)) {
            Collections.shuffle(numbers);
        }
        for (int i = 0; i < playing.size(); i++) {
            GamePlayer participant = playing.get(i);
            participant.aliasNumber(numbers.get(i));
            Player player = participant.player();
            if (player != null) {
                plugin.msg().send(player, "alias-assigned",
                        Ph.of("alias", plugin.nametags().alias(this, participant)));
            }
        }
    }

    private Location nextSpawn() {
        Location location = spawns.get(spawnRotation % spawns.size());
        spawnRotation++;
        return location.clone();
    }

    private static String namesOf(List<GamePlayer> playing, Role role) {
        List<String> names = new ArrayList<>();
        for (GamePlayer participant : playing) {
            if (participant.role() == role) {
                names.add(participant.name());
            }
        }
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private void handOutWeapons() {
        weaponsGiven = true;
        for (GamePlayer participant : participants.values()) {
            Player player = participant.player();
            if (player == null || !participant.alive() || participant.spectator()) {
                continue;
            }
            if (participant.role() == Role.MURDERER || participant.role() == Role.DETECTIVE) {
                equip(participant, player);
                plugin.msg().send(player, "weapon-received-" + participant.role().lower());
            }
        }
    }

    private void equip(GamePlayer participant, Player player) {
        if (participant.role() == Role.MURDERER) {
            Material skin = plugin.stats().equippedMaterial(participant.uuid(), "knife");
            player.getInventory().setItem(plugin.items().slot("knife", 0), plugin.items().knife(skin));
        } else if (participant.role() == Role.DETECTIVE) {
            giveBow(participant, false);
        }
    }

    private void announceRole(GamePlayer participant, Player player) {
        Role role = participant.role();
        if (role == null) {
            return;
        }
        plugin.msg().send(player, "role-assigned-" + role.lower());
        if (plugin.settings().getBoolean(this, "roles.show-title", true)
                && plugin.settings().feature("titles")) {
            player.showTitle(Title.title(
                    plugin.msg().get(player, "role-title-" + role.lower(), Ph.none()),
                    plugin.msg().get(player, "role-subtitle-" + role.lower(), Ph.none()),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
    }

    public void giveBow(GamePlayer participant, boolean announce) {
        Player player = participant.player();
        if (player == null) {
            return;
        }
        participant.hasBow(true);
        Material skin = plugin.stats().equippedMaterial(participant.uuid(), "bow");
        player.getInventory().setItem(plugin.items().slot("bow", 1), plugin.items().bow(skin));
        player.getInventory().setItem(plugin.items().slot("arrow", 9), plugin.items().arrow());
        if (announce) {
            plugin.msg().send(player, "bow-received");
            plugin.sounds().play(player, "bow-received");
            plugin.stats().addBow(participant.uuid());
            if (plugin.settings().feature("titles")) {
                player.showTitle(Title.title(
                        plugin.msg().get(player, "bow-received-title", Ph.none()),
                        plugin.msg().get(player, "bow-received-sub", Ph.none()),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(400))));
            }
        }
    }

    public void dropDetectiveBow(Location location) {
        if (!plugin.settings().getBoolean(this, "bow.detective-bow-drops", true)) {
            return;
        }
        if (location == null || location.getWorld() == null) {
            return;
        }
        ItemStack stack = plugin.items().bow(null);
        Item item = location.getWorld().dropItem(location, stack);
        item.setVelocity(item.getVelocity().zero());
        item.setCanMobPickup(false);
        item.setPickupDelay(0);
        Tags.tag(item, name, Tags.KIND_BOW);
        broadcast("bow-dropped");
        plugin.sounds().playAt(location, "bow-dropped");
    }

    private Location bowDropLocation(Location death, boolean hazard) {
        int voidY = plugin.settings().getInt(this, "death-rules.void-y", 0);
        boolean unreachable = death == null || death.getWorld() == null || hazard || death.getY() <= voidY + 1;
        if (!unreachable) {
            return death;
        }
        org.bukkit.World world = bounds == null ? null : org.bukkit.Bukkit.getWorld(bounds.world());
        if (world != null) {
            Location center = bounds.center(world);
            int top = Math.min(bounds.maxY(), world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ()));
            center.setY(top + 1);
            return center;
        }
        return spectatorSpawn != null ? spectatorSpawn.clone() : death;
    }

    public void dropBowAfterQuit(Location quitLocation) {
        dropDetectiveBow(safeBowSpot(quitLocation));
    }

    // Zufälliger Spawn- oder Goldpunkt, nie in Falle, Todeszone, Flüssigkeit oder über dem Void
    private Location safeBowSpot(Location fallback) {
        List<Location> candidates = new ArrayList<>();
        candidates.addAll(spawns);
        candidates.addAll(goldSpawns);
        java.util.Collections.shuffle(candidates);
        int voidY = plugin.settings().getInt(this, "death-rules.void-y", 0);
        for (Location spot : candidates) {
            if (spot == null || spot.getWorld() == null || spot.getY() <= voidY + 2) {
                continue;
            }
            org.bukkit.block.Block feet = spot.getBlock();
            org.bukkit.block.Block ground = feet.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (feet.isLiquid() || !ground.getType().isSolid()) {
                continue;
            }
            boolean unsafe = false;
            for (dev.paperstream.murder.region.Trap trap : traps.values()) {
                if (trap.area().contains(spot, 2)) {
                    unsafe = true;
                    break;
                }
            }
            for (Zone zone : zones.values()) {
                if (zone.type() != dev.paperstream.murder.region.ZoneType.SAFE && zone.area().contains(spot, 1)) {
                    unsafe = true;
                    break;
                }
            }
            if (!unsafe) {
                return spot.clone().add(0, 0.5, 0);
            }
        }
        return bowDropLocation(fallback, false);
    }

    public boolean pickupBow(Player player, Item item) {
        GamePlayer participant = participant(player.getUniqueId());
        if (participant == null || !participant.alive() || participant.spectator()) {
            return true;
        }
        if (participant.role() == Role.MURDERER) {
            plugin.msg().send(player, "murderer-no-bow");
            return true;
        }
        item.remove();
        participant.role(Role.DETECTIVE);
        giveBow(participant, false);
        plugin.msg().send(player, "bow-picked-up");
        plugin.sounds().play(player, "bow-received");
        plugin.boards().refresh(this);
        return true;
    }

    public void kill(GamePlayer victim, Player killer, String messageKey) {
        if (!victim.alive() || victim.spectator()) {
            return;
        }
        Player player = victim.player();
        if (victim.consumeExtraLife() && player != null) {
            player.setHealth(Math.max(1D, player.getHealth()));
            plugin.msg().send(player, "kali-cleansed", Ph.of("amount", 0));
            return;
        }
        var killEvent = new dev.paperstream.murder.api.event.MurderPlayerKillEvent(name, player, victim.role(), killer, messageKey);
        plugin.getServer().getPluginManager().callEvent(killEvent);
        if (killEvent.isCancelled()) {
            return;
        }
        victim.alive(false);
        Location deathLocation = player != null ? player.getLocation().clone() : null;
        victim.deathLocation(deathLocation);

        boolean anonymous = plugin.settings().getBoolean(this, "nametags.hide-in-death-messages", false);
        if (anonymous) {

            broadcast("death-by-bow".equals(messageKey) || "death-by-detective".equals(messageKey)
                    ? "death-anonymous-shot" : "death-anonymous");
        } else {
            broadcast(messageKey, Ph.of("player", victim.name()));
        }

        if (victim.role() == Role.DETECTIVE) {
            boolean hazard = killer == null && !"death-generic".equals(messageKey);
            dropDetectiveBow(hazard ? safeBowSpot(deathLocation) : bowDropLocation(deathLocation, false));
        }
        if (deathLocation != null && plugin.settings().feature("corpses")) {
            plugin.corpses().spawn(this, victim, deathLocation);
        }
        if (deathLocation != null) {
            plugin.stats().playDeathEffect(victim.uuid(), deathLocation);
        }

        if (killer != null) {
            GamePlayer killerPlayer = participant(killer.getUniqueId());
            if (killerPlayer != null) {
                killerPlayer.addKill();
                plugin.stats().addKill(killer.getUniqueId());
                plugin.rewards().onKill(killer);
            }
        }
        if (victim.role() == Role.MURDERER) {
            broadcast("murderer-killed");
            plugin.sounds().play(onlinePlayers(), "murderer-killed");
        }

        if (player != null) {
            plugin.sounds().play(player, "death-self");
            if (plugin.settings().feature("titles")) {
                player.showTitle(Title.title(
                        plugin.msg().get(player, "death-self", Ph.none()),
                        plugin.msg().get(player, "death-self-sub", Ph.none()),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))));
            }
            plugin.kali().clearPlayer(victim.uuid(), player);
            makeSpectator(victim, player);
            victim.spectator(false);
        }
        plugin.boards().refresh(this);
        checkWin();
    }

    public void checkWin() {
        if (state != GameState.RUNNING) {
            return;
        }
        WinChecker.Result outcome = WinChecker.check(this);
        if (outcome != WinChecker.Result.NONE) {
            end(outcome);
        }
    }

    public void end(WinChecker.Result outcome) {
        if (state == GameState.ENDING) {
            return;
        }
        this.result = outcome;
        state(GameState.ENDING);
        plugin.getServer().getPluginManager().callEvent(new dev.paperstream.murder.api.event.MurderGameEndEvent(name, outcome));
        endingLeft = plugin.settings().getInt(this, "ending-time", 10);

        if (outcome != WinChecker.Result.ABORTED) {
            switch (outcome) {
                case INNOCENTS -> broadcast("win-innocents");
                case MURDERERS -> broadcast("win-murderers");
                default -> broadcast("win-nobody");
            }
            broadcast("was-the-murderer", Ph.of("player", lastMurdererName));
            if (!"-".equals(lastDetectiveName)) {
                broadcast("was-the-detective", Ph.of("player", lastDetectiveName));
            }
            showSummary();
            showTitles(outcome);
            plugin.rewards().onRoundEnd(this, outcome);
            plugin.stats().onRoundEnd(this, outcome);
        }

        if (replayId != null) {
            plugin.replays().stopRecording(replayId, true);
        }
        plugin.boards().refresh(this);
    }

    private void showTitles(WinChecker.Result outcome) {
        if (!plugin.settings().feature("titles")) {
            return;
        }
        for (GamePlayer participant : participants.values()) {
            Player player = participant.player();
            if (player == null) {
                continue;
            }
            boolean won = switch (outcome) {
                case INNOCENTS -> participant.role() != Role.MURDERER;
                case MURDERERS -> participant.role() == Role.MURDERER;
                default -> false;
            };
            player.showTitle(Title.title(
                    plugin.msg().get(player, won ? "win-title-innocents" : "win-title-murderers", Ph.none()),
                    plugin.msg().get(player, "win-subtitle", Ph.of("murderer", lastMurdererName)),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))));
            plugin.sounds().play(player, won ? "win" : "lose");
        }
    }

    private void showSummary() {
        GamePlayer topKiller = null;
        GamePlayer topGold = null;
        for (GamePlayer participant : participants.values()) {
            if (topKiller == null || participant.kills() > topKiller.kills()) {
                topKiller = participant;
            }
            if (topGold == null || participant.goldCollected() > topGold.goldCollected()) {
                topGold = participant;
            }
        }
        broadcast("summary-header");
        if (topKiller != null && topKiller.kills() > 0) {
            broadcast("summary-line", Ph.of(
                    "name", plugin.msg().rawString("summary-most-kills"),
                    "value", topKiller.name() + " (" + topKiller.kills() + ")"));
        }
        if (topGold != null && topGold.goldCollected() > 0) {
            broadcast("summary-line", Ph.of(
                    "name", plugin.msg().rawString("summary-most-gold"),
                    "value", topGold.name() + " (" + topGold.goldCollected() + ")"));
        }
        long seconds = (System.currentTimeMillis() - roundStart) / 1000L;
        broadcast("summary-line", Ph.of(
                "name", plugin.msg().rawString("summary-duration"),
                "value", Text.clock((int) seconds)));
    }

    public void stop(boolean announce) {
        if (announce && state == GameState.RUNNING) {
            broadcast("game-stopped");
        }
        if (replayId != null) {
            plugin.replays().stopRecording(replayId, false);
            replayId = null;
        }
        reset();
    }

    public void reset() {
        plugin.cleanup().resetArena(this);
        result = WinChecker.Result.NONE;
        countdown = 0;
        timeLeft = 0;
        endingLeft = 0;
        replayId = null;
        spawnRotation = 0;
        state(enabled ? GameState.WAITING : GameState.DISABLED);
        stopTicker();
    }

    public void ensureTicker() {
        if (ticker != null && !ticker.isCancelled()) {
            return;
        }

        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        tickCounter = 0;
    }

    private void tick() {
        tickCounter++;
        try {
            plugin.zones().tick(this);
            plugin.boards().tickFast(this);
        } catch (Exception exception) {
            plugin.getLogger().warning("Ticker error in arena " + name + ": " + exception.getMessage());
        }
        if (tickCounter % 4 != 0) {
            return;
        }
        try {
            tickSecond();
        } catch (Exception exception) {
            plugin.getLogger().warning("Second tick error in arena " + name + ": " + exception.getMessage());
        }
    }

    private void tickSecond() {
        switch (state) {
            case WAITING -> {
                if (playerCount() >= minPlayers()) {
                    startCountdown(false);
                }
                if (participants.isEmpty()) {
                    stopTicker();
                }
            }
            case COUNTDOWN -> tickCountdown();
            case RUNNING -> tickRunning();
            case ENDING -> tickEnding();
            default -> {
            }
        }
        plugin.boards().refresh(this);
    }

    private void tickCountdown() {
        if (playerCount() < minPlayers()) {
            cancelCountdown();
            return;
        }
        countdown--;
        if (countdown <= 0) {
            start();
            return;
        }
        if (countdown <= 5 || countdown % 10 == 0 || countdown == countdownTime()) {
            broadcast("countdown-tick", Ph.of("seconds", countdown));
            plugin.sounds().play(onlinePlayers(), countdown <= 5 ? "countdown-final" : "countdown-tick");
        }
    }

    private void tickRunning() {
        if (!weaponsGiven && System.currentTimeMillis() - roundStart
                >= plugin.settings().getInt(this, "roles.weapon-delay", 15) * 1000L) {
            handOutWeapons();
        }
        tickEndgame();
        gold.tick();
        plugin.kali().tick(this);
        plugin.potions().tick(this);
        tickAntiCamp();

        int limit = roundTime();
        if (limit > 0) {
            timeLeft--;
            if (timeLeft <= 0) {
                broadcast("time-over");
                end(WinChecker.onTimeOver());
                return;
            }
        }
        checkWin();
    }

    private void tickAntiCamp() {
        int seconds = plugin.settings().getInt(this, "extras.anti-camp-seconds", 45);
        boolean endgame = timeLeftGlow();
        if (seconds <= 0 && !endgame) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GamePlayer participant : participants.values()) {
            Player player = participant.player();
            if (player == null || !participant.alive() || participant.spectator()) {
                continue;
            }
            boolean camping = seconds > 0 && now - participant.lastMove() > seconds * 1000L;
            boolean revealGlow = revealed && plugin.settings().getBoolean(this, "extras.reveal-glow", false);
            player.setGlowing(camping || endgame || revealGlow);
        }
    }

    private void tickEndgame() {
        int revealAt = plugin.settings().getInt(this, "extras.reveal-at-survivors", 2);
        int speedAt = plugin.settings().getInt(this, "extras.murderer-speed-at-survivors", 1);
        int survivors = aliveOf(Role.INNOCENT).size() + aliveOf(Role.DETECTIVE).size();
        if (revealAt > 0 && survivors <= revealAt) {
            if (!revealed) {
                revealed = true;
                broadcast("players-revealed");
            }
            if (plugin.settings().getBoolean(this, "extras.reveal-glow", false)) {
                for (GamePlayer participant : participants.values()) {
                    Player player = participant.player();
                    if (player != null && participant.alive() && !participant.spectator()) {
                        player.setGlowing(true);
                    }
                }
            }
        }
        if (speedAt > 0 && survivors <= speedAt) {
            for (GamePlayer murderer : aliveOf(Role.MURDERER)) {
                Player player = murderer.player();
                if (player != null) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SPEED, 40, 0, false, false, true));
                }
            }
        }
    }

    private boolean timeLeftGlow() {
        int lastMan = plugin.settings().getInt(this, "extras.last-man-standing-seconds", 60);
        return lastMan > 0 && roundTime() > 0 && timeLeft <= lastMan;
    }

    private void tickEnding() {
        endingLeft--;
        if (endingLeft == 3 || endingLeft == 5) {
            broadcast("next-round", Ph.of("seconds", endingLeft));
        }
        if (endingLeft <= 0) {
            offerReplay();
            for (Player player : onlinePlayers()) {
                leave(player, true);
            }
            reset();
        }
    }

    private void offerReplay() {
        if (replayId == null || !plugin.settings().getBoolean(this, "replay.offer-after-round", true)) {
            return;
        }
        for (Player player : onlinePlayers()) {
            plugin.msg().send(player, "replay-offer");
        }
    }

    public List<String> missingSetup() {
        List<String> missing = new ArrayList<>();
        if (plugin.arenas().globalLobby() == null && lobby == null) {
            missing.add("lobby");
        }
        if (bounds == null) {
            missing.add("bounds");
        }
        if (spawns.size() < Math.min(maxPlayers(), 2)) {
            missing.add("spawns");
        }
        if (spectatorSpawn == null) {
            missing.add("spectator");
        }
        if (goldSpawns.isEmpty()) {
            missing.add("gold");
        }
        return missing;
    }

    public boolean ready() {
        return missingSetup().isEmpty();
    }

    public void load(ConfigurationSection section) {
        displayName = section.getString("displayname", name);
        author = section.getString("author", "");
        Material material = dev.paperstream.murder.util.Registries.material(section.getString("icon"));
        icon = material == null ? Material.PAPER : material;
        enabled = section.getBoolean("enabled", false);
        minPlayers = section.contains("min-players") ? section.getInt("min-players") : null;
        maxPlayers = section.contains("max-players") ? section.getInt("max-players") : null;
        roundTime = section.contains("time") ? section.getInt("time") : null;
        countdownTime = section.contains("countdown") ? section.getInt("countdown") : null;
        forcedMurderers = section.getInt("murderers", 0);
        forcedDetectives = section.getInt("detectives", -1);
        overrides = section.getConfigurationSection("overrides");

        spawns.clear();
        for (Object raw : section.getList("spawns", List.of())) {
            Location location = Locations.fromMap(raw);
            if (location != null) {
                spawns.add(location);
            }
        }
        goldSpawns.clear();
        for (Object raw : section.getList("gold", List.of())) {
            Location location = Locations.fromMap(raw);
            if (location != null) {
                goldSpawns.add(location);
            }
        }
        potionStations.clear();
        for (Object raw : section.getList("potion-stations", List.of())) {
            Location location = Locations.fromMap(raw);
            if (location != null) {
                potionStations.add(location);
            }
        }
        spectatorSpawn = Locations.fromMap(section.get("spectator"));
        lobby = Locations.fromMap(section.get("lobby"));
        altar = Locations.fromMap(section.get("altar"));

        ConfigurationSection boundsSection = section.getConfigurationSection("bounds");
        bounds = Cuboid.fromSection(boundsSection, boundsSection == null ? null : boundsSection.getString("world"));

        zones.clear();
        ConfigurationSection zoneSection = section.getConfigurationSection("zones");
        if (zoneSection != null) {
            String world = bounds == null ? null : bounds.world();
            for (String key : zoneSection.getKeys(false)) {
                Zone zone = Zone.fromSection(key, zoneSection.getConfigurationSection(key), world);
                if (zone != null) {
                    zones.put(key.toLowerCase(java.util.Locale.ROOT), zone);
                }
            }
        }
        traps.clear();
        ConfigurationSection trapSection = section.getConfigurationSection("traps");
        if (trapSection != null) {
            String world = bounds == null ? null : bounds.world();
            for (String key : trapSection.getKeys(false)) {
                dev.paperstream.murder.region.Trap trap = dev.paperstream.murder.region.Trap.fromSection(
                        key, trapSection.getConfigurationSection(key), world);
                if (trap != null) {
                    traps.put(key.toLowerCase(java.util.Locale.ROOT), trap);
                }
            }
        }
        state = enabled ? GameState.WAITING : GameState.DISABLED;
    }

    public void save(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("displayname", displayName);
        section.set("author", author);
        section.set("icon", icon.name());
        section.set("min-players", minPlayers);
        section.set("max-players", maxPlayers);
        section.set("time", roundTime);
        section.set("countdown", countdownTime);
        section.set("murderers", forcedMurderers == 0 ? null : forcedMurderers);
        section.set("detectives", forcedDetectives < 0 ? null : forcedDetectives);

        List<Map<String, Object>> spawnList = new ArrayList<>();
        for (Location location : spawns) {
            spawnList.add(Locations.toMap(location));
        }
        section.set("spawns", spawnList);

        List<Map<String, Object>> goldList = new ArrayList<>();
        for (Location location : goldSpawns) {
            goldList.add(Locations.toMap(location));
        }
        section.set("gold", goldList);

        List<Map<String, Object>> potionList = new ArrayList<>();
        for (Location location : potionStations) {
            potionList.add(Locations.toMap(location));
        }
        section.set("potion-stations", potionList.isEmpty() ? null : potionList);

        section.set("spectator", spectatorSpawn == null ? null : Locations.toMap(spectatorSpawn));
        section.set("lobby", lobby == null ? null : Locations.toMap(lobby));
        section.set("altar", altar == null ? null : Locations.toMap(altar));
        section.set("bounds", bounds == null ? null : bounds.toMap(true));

        if (overrides != null && !overrides.getKeys(false).isEmpty()) {
            ConfigurationSection copy = section.createSection("overrides");
            for (String key : overrides.getKeys(true)) {
                if (!overrides.isConfigurationSection(key)) {
                    copy.set(key, overrides.get(key));
                }
            }
            overrides = copy;
        }

        section.set("zones", null);
        if (!zones.isEmpty()) {
            ConfigurationSection zoneSection = section.createSection("zones");
            for (Zone zone : zones.values()) {
                zoneSection.set(zone.name(), zone.toMap());
            }
        }

        section.set("traps", null);
        if (!traps.isEmpty()) {
            ConfigurationSection trapSection = section.createSection("traps");
            for (dev.paperstream.murder.region.Trap trap : traps.values()) {
                trapSection.set(trap.name(), trap.toMap());
            }
        }
    }

    public void copyTo(Arena target) {
        target.displayName = displayName;
        target.author = author;
        target.icon = icon;
        target.minPlayers = minPlayers;
        target.maxPlayers = maxPlayers;
        target.roundTime = roundTime;
        target.countdownTime = countdownTime;
        target.forcedMurderers = forcedMurderers;
        target.forcedDetectives = forcedDetectives;
        target.spawns.addAll(spawns);
        target.goldSpawns.addAll(goldSpawns);
        target.potionStations.addAll(potionStations);
        target.spectatorSpawn = spectatorSpawn;
        target.lobby = lobby;
        target.altar = altar;
        target.bounds = bounds;
        target.zones.putAll(zones);
        target.overrides = overrides;
    }
}
