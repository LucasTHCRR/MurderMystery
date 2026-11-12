package com.lucas.murder;

import com.lucas.murder.cleanup.CleanupService;
import com.lucas.murder.command.HelpService;
import com.lucas.murder.command.MurderCommand;
import com.lucas.murder.command.SetupWizard;
import com.lucas.murder.config.ConfigManager;
import com.lucas.murder.config.ItemService;
import com.lucas.murder.config.Messages;
import com.lucas.murder.config.Settings;
import com.lucas.murder.config.SoundService;
import com.lucas.murder.extras.kali.KaliManager;
import com.lucas.murder.extras.potion.PotionManager;
import com.lucas.murder.game.ArenaManager;
import com.lucas.murder.game.ChatManager;
import com.lucas.murder.game.CombatListener;
import com.lucas.murder.game.ReconnectManager;
import com.lucas.murder.game.RoleAssigner;
import com.lucas.murder.game.VoteManager;
import com.lucas.murder.gui.Menus;
import com.lucas.murder.hook.AdvancedReplayService;
import com.lucas.murder.hook.CorpseProviderRegistry;
import com.lucas.murder.hook.HookManager;
import com.lucas.murder.hook.NoopReplayService;
import com.lucas.murder.hook.NpcProviderRegistry;
import com.lucas.murder.hook.ReplayService;
import com.lucas.murder.listener.CommandBlockListener;
import com.lucas.murder.listener.ConnectionListener;
import com.lucas.murder.listener.DeathRuleListener;
import com.lucas.murder.listener.GameListener;
import com.lucas.murder.listener.MenuListener;
import com.lucas.murder.listener.NpcListener;
import com.lucas.murder.listener.SignListener;
import com.lucas.murder.listener.SignManager;
import com.lucas.murder.listener.WandListener;
import com.lucas.murder.npc.NpcManager;
import com.lucas.murder.region.SelectionManager;
import com.lucas.murder.region.ZoneTicker;
import com.lucas.murder.scoreboard.BoardManager;
import com.lucas.murder.scoreboard.NametagManager;
import com.lucas.murder.stats.RewardService;
import com.lucas.murder.stats.StatsManager;
import com.lucas.murder.util.AuditLog;
import com.lucas.murder.util.Cooldowns;
import com.lucas.murder.util.Tags;
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
        manager.registerEvents(new com.lucas.murder.listener.ProtectionListener(this), this);
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
        manager.registerEvents(new com.lucas.murder.listener.KaliChatListener(this), this);
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
