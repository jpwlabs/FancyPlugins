package de.oliver.fancynpcs;

import com.fancyinnovations.config.featureflags.FeatureFlag;
import com.fancyinnovations.config.featureflags.FeatureFlagConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.oliver.jpw.logging.ExtendedFancyLogger;
import de.oliver.jpw.logging.LogLevel;
import de.oliver.jpw.logging.appender.Appender;
import de.oliver.jpw.logging.appender.ConsoleAppender;
import de.oliver.jpw.logging.appender.JsonAppender;
import de.oliver.jpw.logging.properties.ThrowableProperty;
import de.oliver.fancylib.FancyLib;
import de.oliver.fancylib.VersionConfig;
import de.oliver.fancylib.logging.PluginMiddleware;
import de.oliver.fancylib.serverSoftware.ServerSoftware;
import de.oliver.fancylib.serverSoftware.schedulers.BukkitScheduler;
import de.oliver.fancylib.serverSoftware.schedulers.FancyScheduler;
import de.oliver.fancylib.serverSoftware.schedulers.FoliaScheduler;
import de.oliver.fancylib.translations.Language;
import de.oliver.fancylib.translations.TextConfig;
import de.oliver.fancylib.translations.Translator;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import de.oliver.fancynpcs.api.actions.types.*;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.skins.SkinManager;
import de.oliver.fancynpcs.commands.CloudCommandManager;
import de.oliver.fancynpcs.listeners.*;
import de.oliver.fancynpcs.skins.SkinManagerImpl;
import de.oliver.fancynpcs.skins.SkinUtils;
import de.oliver.fancynpcs.skins.cache.SkinCacheFile;
import de.oliver.fancynpcs.skins.cache.SkinCacheMemory;
import de.oliver.fancynpcs.skins.uuidcache.UUIDFileCache;
import de.oliver.fancynpcs.tests.PlaceholderApiEnv;
import de.oliver.fancynpcs.tracker.TurnToPlayerTracker;
import de.oliver.fancynpcs.tracker.VisibilityTracker;
import de.oliver.fancynpcs.utils.OldSkinCacheMigrator;
import de.oliver.fancynpcs.v1_21_6.Npc_1_21_6;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FancyNpcs extends JavaPlugin implements FancyNpcsPlugin {

    public static final FeatureFlag PLAYER_NPCS_FEATURE_FLAG = new FeatureFlag("player-npcs", "Every player can only manage the npcs they have created", false);
    public static final FeatureFlag USE_NATIVE_THREADS_FEATURE_FLAG = new FeatureFlag("use-native-threads", "Use native threads instead of virtual threads.", false);
    public static final FeatureFlag ENABLE_DEBUG_MODE_FEATURE_FLAG = new FeatureFlag("enable-debug-mode", "Enable debug mode", false);
    public static final FeatureFlag ENABLE_FOLIA_VISIBILITY_FIX_FEATURE_FLAG = new FeatureFlag("enable-folia-visibility-fix", "When enabled, all npcs will respawn after 100ms when they should spawn", false);
    public static final FeatureFlag USE_MINECRAFT_USERCACHE_FEATURE_FLAG = new FeatureFlag("use-minecraft-usercache", "Include the content of usercache.json to the username->uuid cache", false);

    private static FancyNpcs instance;
    private final ExtendedFancyLogger fancyLogger;
    private final ScheduledExecutorService npcThread;
    private final FancyScheduler scheduler;
    private final FancyNpcsConfigImpl config;
    private final VersionConfig versionConfig;
    private final FeatureFlagConfig featureFlagConfig;
    private CloudCommandManager commandManager;
    private TextConfig textConfig;
    private Translator translator;
    private Function<NpcData, Npc> npcAdapter;
    private NpcManagerImpl npcManager;
    private AttributeManagerImpl attributeManager;
    private SkinManagerImpl skinManager;
    private ActionManagerImpl actionManager;
    private VisibilityTracker visibilityTracker;
    private boolean usingPlotSquared;

    public FancyNpcs() {
        instance = this;

        Appender consoleAppender = new ConsoleAppender("[{loggerName}] ({threadName}) {logLevel}: {message}");
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
        File logsFile = new File("plugins/FancyNpcs/logs/FN-logs-" + date + ".txt");
        if (!logsFile.exists()) {
            try {
                logsFile.getParentFile().mkdirs();
                logsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JsonAppender jsonAppender = new JsonAppender(false, false, true, logsFile.getPath());
        this.fancyLogger = new ExtendedFancyLogger(
                "FancyNpcs",
                LogLevel.INFO,
                List.of(consoleAppender, jsonAppender),
                List.of(new PluginMiddleware(this))
        );

        this.npcThread = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("FancyNpcs-Npcs")
                        .build()
        );
        this.scheduler = ServerSoftware.isFolia()
                ? new FoliaScheduler(instance)
                : new BukkitScheduler(instance);
        this.config = new FancyNpcsConfigImpl();
        this.versionConfig = new VersionConfig(this);

        this.featureFlagConfig = new FeatureFlagConfig(this);
    }

    public static FancyNpcs getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        // Load feature flags
        featureFlagConfig.addFeatureFlag(PLAYER_NPCS_FEATURE_FLAG);
        featureFlagConfig.addFeatureFlag(USE_NATIVE_THREADS_FEATURE_FLAG);
        featureFlagConfig.addFeatureFlag(ENABLE_DEBUG_MODE_FEATURE_FLAG);
        featureFlagConfig.addFeatureFlag(ENABLE_FOLIA_VISIBILITY_FIX_FEATURE_FLAG);
        featureFlagConfig.addFeatureFlag(USE_MINECRAFT_USERCACHE_FEATURE_FLAG);
        featureFlagConfig.load();

        if (ENABLE_DEBUG_MODE_FEATURE_FLAG.isEnabled()) {
            fancyLogger.setCurrentLevel(LogLevel.DEBUG);
        }

        String mcVersion = Bukkit.getMinecraftVersion();

        npcAdapter = switch (mcVersion) {
            case "1.21.8" -> Npc_1_21_6::new;
            default -> null;
        };

        if (npcAdapter == null) {
            fancyLogger.error("Unsupported minecraft server version.");
            getLogger().warning("--------------------------------------------------");
            getLogger().warning("Unsupported minecraft server version.");
            getLogger().warning("This JPW build only supports Paper 1.21.8");
            getLogger().warning("Disabling the FancyNpcs plugin.");
            getLogger().warning("--------------------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        npcManager = new NpcManagerImpl(this, npcAdapter);
    }

    @Override
    public void onEnable() {
        if (npcAdapter == null) {
            return;
        }

        new FancyLib(instance); // Initialize FancyLib

        String mcVersion = Bukkit.getMinecraftVersion();

        config.reload();

        attributeManager = new AttributeManagerImpl();
        actionManager = new ActionManagerImpl();
        actionManager.registerAction(new MessageAction());
        actionManager.registerAction(new PlayerCommandAction());
        actionManager.registerAction(new ConsoleCommandAction());
        actionManager.registerAction(new SendToServerAction());
        actionManager.registerAction(new WaitAction());
        actionManager.registerAction(new ExecuteRandomActionAction());
        actionManager.registerAction(new BlockUntilDoneAction());
        actionManager.registerAction(new NeedPermissionAction());
        actionManager.registerAction(new NeedItemAction());
        actionManager.registerAction(new PlaySoundAction());

        skinManager = new SkinManagerImpl(new UUIDFileCache(), new SkinCacheFile(), new SkinCacheMemory());
        OldSkinCacheMigrator.migrate();

        textConfig = new TextConfig("#E33239", "#AD1D23", "#81E366", "#E3CA66", "#E36666", "");
        translator = new Translator(textConfig);
        translator.loadLanguages(getDataFolder().getAbsolutePath());
        final Language selectedLanguage = translator.getLanguages().stream()
                .filter(language -> language.getLanguageName().equals(config.getLanguage()))
                .findFirst().orElse(translator.getFallbackLanguage());
        translator.setSelectedLanguage(selectedLanguage);

        versionConfig.load();

        if (!ServerSoftware.isPaper()) {
            fancyLogger.warn("You are not using Paper as server software.");
            getLogger().warning("--------------------------------------------------");
            getLogger().warning("It is recommended to use Paper as server software.");
            getLogger().warning("Because you are not using paper, the plugin");
            getLogger().warning("might not work correctly.");
            getLogger().warning("--------------------------------------------------");
        }

        PluginManager pluginManager = Bukkit.getPluginManager();
        usingPlotSquared = pluginManager.isPluginEnabled("PlotSquared");

        // register listeners
        pluginManager.registerEvents(new PlayerJoinListener(), instance);
        pluginManager.registerEvents(new PlayerQuitListener(), instance);
        pluginManager.registerEvents(new PlayerTeleportListener(), instance);
        pluginManager.registerEvents(new PlayerChangedWorldListener(), instance);
        if (Bukkit.getMinecraftVersion().equals("1.21.8")) {
            getServer().getPluginManager().registerEvents(new PlayerLoadedListener(), this);
        }

        pluginManager.registerEvents(new PlayerUseUnknownEntityListener(), instance);

        if (PLAYER_NPCS_FEATURE_FLAG.isEnabled()) {
            pluginManager.registerEvents(new PlayerNpcsListener(), instance);
        }

        // using bungee plugin channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // load config
        scheduler.runTaskLater(null, 20L * 5, () -> npcManager.loadNpcs());

        visibilityTracker = new VisibilityTracker();

        npcThread.scheduleAtFixedRate(new TurnToPlayerTracker(), 0, 50, TimeUnit.MILLISECONDS);
        npcThread.scheduleAtFixedRate(visibilityTracker, 0, (config.getNpcUpdateVisibilityInterval() * 50L), TimeUnit.MILLISECONDS);

        int autosaveInterval = config.getAutoSaveInterval();
        if (config.isEnableAutoSave() && config.getAutoSaveInterval() > 0) {
            scheduler.runTaskTimerAsynchronously(60L * 20L, autosaveInterval * 60L * 20L, () -> npcManager.saveNpcs(false));
        }

        int npcUpdateInterval = config.getNpcUpdateInterval();
        npcThread.scheduleAtFixedRate(() -> {
            final List<Npc> npcs = new ArrayList<>(npcManager.getAllNpcs());
            for (final Npc npc : npcs) {
                try {
                    boolean shouldUpdate = npc.getData().getDisplayName() != null && !npc.getData().getDisplayName().isBlank() && SkinUtils.isPlaceholder(npc.getData().getDisplayName());

                    if (npc.getData().getSkinData() != null) {
                        final String skinID = npc.getData().getSkinData().getIdentifier();
                        if (!skinID.isEmpty() && SkinUtils.isPlaceholder(skinID)) {
                            final SkinData skinData = skinManager.getByIdentifier(skinID, npc.getData().getSkinData().getVariant());
                            skinData.setIdentifier(skinID);
                            npc.getData().setSkinData(skinData);
                            shouldUpdate = true;
                        }
                    }

                    if (shouldUpdate) {
                        npc.removeForAll();
                        npc.create();
                        npc.spawnForAll();
                    }
                } catch (final Throwable thr) {
                    fancyLogger.error("An error occurred while updating '" + npc.getData().getName() + "' NPC.", ThrowableProperty.of(thr));
                }
            }
        }, 30, npcUpdateInterval, TimeUnit.SECONDS);

        // Creating new instance of CloudCommandManager and registering all needed components.
        // NOTE: Brigadier is disabled by default. More detailed information about that can be found in CloudCommandManager class.
        if (config.isRegisterCommands()) {
            commandManager = new CloudCommandManager(this, false)
                    .registerArguments()
                    .registerExceptionHandlers()
                    .registerCommands();
        } else {
            getLogger().warning("Commands and related components have not been registered. This can be changed by setting 'register_commands' to true, and restarting the server.");
        }

        if (ENABLE_DEBUG_MODE_FEATURE_FLAG.isEnabled() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceholderApiEnv.registerPlaceholders();
        }

        fancyLogger.info("FancyNpcs (" + versionConfig.getVersion() + ") has been enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        if (npcManager != null) {
            npcManager.saveNpcs(true);
        }

        fancyLogger.info("FancyNpcs has been disabled.");
    }

    @Override
    public Thread newThread(String name, Runnable runnable) {
        if (USE_NATIVE_THREADS_FEATURE_FLAG.isEnabled()) {
            return new Thread(runnable, name);
        }

        return Thread.ofVirtual().name(name).unstarted(runnable);
    }

    @Override
    public ExtendedFancyLogger getFancyLogger() {
        return fancyLogger;
    }

    @Override
    public ScheduledExecutorService getNpcThread() {
        return npcThread;
    }

    @Override
    public Function<NpcData, Npc> getNpcAdapter() {
        return npcAdapter;
    }

    @Override
    public FancyScheduler getScheduler() {
        return scheduler;
    }

    public NpcManagerImpl getNpcManagerImpl() {
        return npcManager;
    }

    @Override
    public NpcManager getNpcManager() {
        return npcManager;
    }

    @Override
    public AttributeManagerImpl getAttributeManager() {
        return attributeManager;
    }

    @Override
    public SkinManager getSkinManager() {
        return skinManager;
    }

    public SkinManagerImpl getSkinManagerImpl() {
        return skinManager;
    }

    @Override
    public ActionManagerImpl getActionManager() {
        return actionManager;
    }

    @Override
    public FancyNpcsConfigImpl getFancyNpcConfig() {
        return config;
    }

    public VersionConfig getVersionConfig() {
        return versionConfig;
    }

    public CloudCommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public Translator getTranslator() {
        return translator;
    }

    public TextConfig getTextConfig() {
        return textConfig;
    }

    @Override
    public FeatureFlagConfig getFeatureFlagConfig() {
        return featureFlagConfig;
    }

    public VisibilityTracker getVisibilityTracker() {
        return visibilityTracker;
    }

    public boolean isUsingPlotSquared() {
        return usingPlotSquared;
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }
}
