package dev.paperstream.murder;

import dev.paperstream.murder.cleanup.CleanupService;
import dev.paperstream.murder.command.HelpService;
import dev.paperstream.murder.command.MurderCommand;
import dev.paperstream.murder.command.SetupWizard;
import dev.paperstream.murder.config.ConfigManager;
import dev.paperstream.murder.config.ItemService;
import dev.paperstream.murder.config.Messages;
import dev.paperstream.murder.config.Settings;
import dev.paperstream.murder.config.SoundService;
import dev.paperstream.murder.extras.kali.KaliManager;
import dev.paperstream.murder.extras.potion.PotionManager;
import dev.paperstream.murder.game.ArenaManager;
import dev.paperstream.murder.game.ChatManager;
import dev.paperstream.murder.game.CombatListener;
import dev.paperstream.murder.game.ReconnectManager;
import dev.paperstream.murder.game.RoleAssigner;
import dev.paperstream.murder.game.VoteManager;
import dev.paperstream.murder.gui.Menus;
import dev.paperstream.murder.hook.AdvancedReplayService;
import dev.paperstream.murder.hook.CorpseProviderRegistry;
import dev.paperstream.murder.hook.HookManager;
import dev.paperstream.murder.hook.NoopReplayService;
import dev.paperstream.murder.hook.NpcProviderRegistry;
import dev.paperstream.murder.hook.ReplayService;
import dev.paperstream.murder.listener.CommandBlockListener;
import dev.paperstream.murder.listener.ConnectionListener;
import dev.paperstream.murder.listener.DeathRuleListener;
import dev.paperstream.murder.listener.GameListener;
import dev.paperstream.murder.listener.MenuListener;
import dev.paperstream.murder.listener.NpcListener;
import dev.paperstream.murder.listener.SignListener;
import dev.paperstream.murder.listener.SignManager;
import dev.paperstream.murder.listener.WandListener;
import dev.paperstream.murder.npc.NpcManager;
import dev.paperstream.murder.region.SelectionManager;
import dev.paperstream.murder.region.ZoneTicker;
import dev.paperstream.murder.scoreboard.BoardManager;
import dev.paperstream.murder.scoreboard.NametagManager;
import dev.paperstream.murder.stats.RewardService;
import dev.paperstream.murder.stats.StatsManager;
import dev.paperstream.murder.util.AuditLog;
import dev.paperstream.murder.util.Cooldowns;
import dev.paperstream.murder.util.Tags;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MurderPlugin extends JavaPlugin {

    private static MurderPlugin instance;

    private ConfigManager configs;
    private SoundService sounds;
    private ItemService items;
    private Cooldowns cooldowns;
    private AuditLog audit;

    private ArenaManager arenas;
    private RoleAssigner roles;
    private VoteManager votes;
    private SelectionManager selections;
    private ZoneTicker zones;
    private dev.paperstream.murder.region.TrapManager traps;
    private DeathRuleListener deathRules;
    private ChatManager chat;
    private ReconnectManager reconnect;

    private BoardManager boards;
    private NametagManager nametags;
    private SignManager signs;

    private HookManager hooks;
    private NpcProviderRegistry npcProviders;
    private CorpseProviderRegistry corpses;
    private NpcManager npcs;
    private ReplayService replays;

    private StatsManager stats;
    private RewardService rewards;
    private KaliManager kali;
    private PotionManager potions;

    private CleanupService cleanup;
    private SetupWizard wizard;
    private HelpService help;
    private Menus menus;

    public static MurderPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        Tags.init(this);

        configs = new ConfigManager(this);
        sounds = new SoundService(configs);
        items = new ItemService(configs);
        cooldowns = new Cooldowns();
        audit = new AuditLog(this,
                configs.config().getBoolean("audit-log.enabled", true),
                configs.config().getString("audit-log.file", "logs/admin.log"));

        arenas = new ArenaManager(this);
        roles = new RoleAssigner(this);
        votes = new VoteManager(this);
        selections = new SelectionManager();
        zones = new ZoneTicker(this);
        dev.paperstream.murder.api.MurderMysteryAPI.init(this);
        traps = new dev.paperstream.murder.region.TrapManager(this);
        deathRules = new DeathRuleListener(this);
        chat = new ChatManager(this);
        reconnect = new ReconnectManager(this);

        boards = new BoardManager(this);
        nametags = new NametagManager(this);
        signs = new SignManager(this);

        hooks = new HookManager(this);
        npcProviders = new NpcProviderRegistry(this);
        corpses = new CorpseProviderRegistry(this);
        npcs = new NpcManager(this);

        stats = new StatsManager(this);
        rewards = new RewardService(this);
        kali = new KaliManager(this);
        potions = new PotionManager(this);

        cleanup = new CleanupService(this);
        wizard = new SetupWizard(this);
        help = new HelpService(this);
        menus = new Menus(this);

        stats.load();
        arenas.load();
        signs.load();

        registerCommand();
        registerListeners();

        // Provider erst nach allen anderen Plugins suchen, sonst wählt AUTO fälschlich INTERNAL
        getServer().getScheduler().runTask(this, this::lateSetup);
        startTicker();

        getLogger().info("MurderMystery enabled - by LucasTHCR (dc.gg/paperstream)");
    }

    private void lateSetup() {
        hooks.detect();
        npcProviders.detect();
        corpses.detect();
        replays = hooks.isEnabled("AdvancedReplay")
                ? new AdvancedReplayService(this) : new NoopReplayService();
        if (replays.isAvailable() && settings().getBoolean(null, "replay.cleanup-on-start", true)) {
            int removed = replays.cleanup(
                    settings().getInt(null, "replay.keep-days", 3),
                    settings().getInt(null, "replay.keep-max", 200));
            if (removed > 0) {
                getLogger().info("Removed " + removed + " old replays.");
            }
        }
        cleanup.cleanupOnStartup();
        npcs.load();
        npcs.spawnAll();
        signs.updateAll();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("murder");
        if (command == null) {
            getLogger().severe("The command /murder is missing from plugin.yml - the plugin is useless like this.");
            return;
        }
        MurderCommand executor = new MurderCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        var manager = getServer().getPluginManager();
        manager.registerEvents(new dev.paperstream.murder.listener.ProtectionListener(this), this);
        manager.registerEvents(traps, this);
        manager.registerEvents(new GameListener(this), this);
        manager.registerEvents(new CombatListener(this), this);
        manager.registerEvents(deathRules, this);
        manager.registerEvents(new CommandBlockListener(this), this);
        manager.registerEvents(new ConnectionListener(this), this);
        manager.registerEvents(new WandListener(this), this);
        manager.registerEvents(new SignListener(this), this);
        manager.registerEvents(new NpcListener(this), this);
        manager.registerEvents(new MenuListener(), this);
        manager.registerEvents(chat, this);
        manager.registerEvents(new dev.paperstream.murder.listener.KaliChatListener(this), this);
    }

    private void startTicker() {
        int saveInterval = Math.max(30, settings().getInt(null, "stats.save-interval-seconds", 300));
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                npcs.tick();
                reconnect.tick();
            } catch (Exception exception) {
                getLogger().warning("Global ticker error: " + exception.getMessage());
            }
        }, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, () -> stats.saveAsync(),
                saveInterval * 20L, saveInterval * 20L);
    }

    @Override
    public void onDisable() {

        // Alles synchron, beim Abschalten läuft der Scheduler nicht mehr
        step("restore the traps", traps, dev.paperstream.murder.region.TrapManager::restoreAll);
        step("shut the arenas down", arenas, ArenaManager::shutdown);
        step("despawn the NPCs", npcs, NpcManager::despawnAll);
        step("remove the corpses", corpses, CorpseProviderRegistry::removeAll);
        step("clear Kali", kali, KaliManager::clearAll);
        step("clear the potions", potions, PotionManager::clearAll);
        step("reset the scoreboards", boards, BoardManager::reset);
        step("save the arenas", arenas, ArenaManager::save);
        step("save the NPCs", npcs, NpcManager::save);
        step("save the signs", signs, SignManager::save);
        step("save the stats", stats, StatsManager::save);
        getLogger().info("MurderMystery disabled.");
    }

    private <T> void step(String what, T subsystem, java.util.function.Consumer<T> action) {
        if (subsystem == null) {
            return;
        }
        try {
            action.accept(subsystem);
        } catch (Exception exception) {
            getLogger().warning("Could not " + what + ": " + exception.getMessage());
        }
    }

    public String reloadAll() {
        String error = configs.reloadAll();
        sounds = new SoundService(configs);
        items = new ItemService(configs);
        audit = new AuditLog(this,
                configs.config().getBoolean("audit-log.enabled", true),
                configs.config().getString("audit-log.file", "logs/admin.log"));
        signs.load();
        signs.updateAll();
        hooks.detect();
        return error;
    }

    public ConfigManager configs() {
        return configs;
    }

    public Messages msg() {
        return configs.messages();
    }

    public Settings settings() {
        return configs.settings();
    }

    public SoundService sounds() {
        return sounds;
    }

    public ItemService items() {
        return items;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public AuditLog audit() {
        return audit;
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public RoleAssigner roles() {
        return roles;
    }

    public VoteManager votes() {
        return votes;
    }

    public SelectionManager selections() {
        return selections;
    }

    public dev.paperstream.murder.region.TrapManager traps() {
        return traps;
    }

    public ZoneTicker zones() {
        return zones;
    }

    public DeathRuleListener deathRules() {
        return deathRules;
    }

    public ChatManager chat() {
        return chat;
    }

    public ReconnectManager reconnect() {
        return reconnect;
    }

    public BoardManager boards() {
        return boards;
    }

    public NametagManager nametags() {
        return nametags;
    }

    public SignManager signs() {
        return signs;
    }

    public HookManager hooks() {
        return hooks;
    }

    public NpcProviderRegistry npcProviders() {
        return npcProviders;
    }

    public CorpseProviderRegistry corpses() {
        return corpses;
    }

    public NpcManager npcs() {
        return npcs;
    }

    public ReplayService replays() {
        return replays == null ? new NoopReplayService() : replays;
    }

    public StatsManager stats() {
        return stats;
    }

    public RewardService rewards() {
        return rewards;
    }

    public KaliManager kali() {
        return kali;
    }

    public PotionManager potions() {
        return potions;
    }

    public CleanupService cleanup() {
        return cleanup;
    }

    public SetupWizard wizard() {
        return wizard;
    }

    public HelpService help() {
        return help;
    }

    public Menus menus() {
        return menus;
    }
}
