package de.oliver.fancyholograms;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.oliver.jpw.logging.ExtendedFancyLogger;
import de.oliver.jpw.logging.LogLevel;
import de.oliver.jpw.logging.appender.Appender;
import de.oliver.jpw.logging.appender.ConsoleAppender;
import de.oliver.jpw.logging.appender.JsonAppender;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramConfiguration;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.HologramStorage;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.commands.FancyHologramsCMD;
import de.oliver.fancyholograms.commands.FancyHologramsTestCMD;
import de.oliver.fancyholograms.commands.HologramCMD;
import de.oliver.fancyholograms.hologram.version.HologramImpl;
import de.oliver.fancyholograms.listeners.*;
import de.oliver.fancyholograms.storage.FlatFileHologramStorage;
import de.oliver.fancyholograms.storage.converter.FHConversionRegistry;
import de.oliver.fancyholograms.util.PluginUtils;
import de.oliver.fancylib.FancyLib;
import de.oliver.fancylib.VersionConfig;
import de.oliver.fancylib.logging.PluginMiddleware;
import de.oliver.fancylib.serverSoftware.ServerSoftware;
import de.oliver.fancysitula.api.IFancySitula;
import de.oliver.fancysitula.api.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class FancyHolograms extends JavaPlugin implements FancyHologramsPlugin {

    private static @Nullable FancyHolograms INSTANCE;
    private final ExtendedFancyLogger fancyLogger;
    private final VersionConfig versionConfig = new VersionConfig(this);
    private final ScheduledExecutorService hologramThread = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("FancyHolograms-Holograms")
                    .build()
    );
    private final ExecutorService fileStorageExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setPriority(Thread.MIN_PRIORITY + 1)
                    .setNameFormat("FancyHolograms-FileStorageExecutor")
                    .build()
    );
    private HologramConfiguration configuration = new FancyHologramsConfiguration();
    private HologramStorage hologramStorage = new FlatFileHologramStorage();
    private @Nullable HologramManagerImpl hologramsManager;

    public FancyHolograms() {
        INSTANCE = this;

        Appender consoleAppender = new ConsoleAppender("[{loggerName}] ({threadName}) {logLevel}: {message}");
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
        File logsFile = new File("plugins/FancyHolograms/logs/FH-logs-" + date + ".txt");
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
                "FancyHolograms",
                LogLevel.INFO,
                List.of(consoleAppender, jsonAppender),
                List.of(new PluginMiddleware(this))
        );
    }

    public static @NotNull FancyHolograms get() {
        return Objects.requireNonNull(INSTANCE, "plugin is not initialized");
    }

    public static boolean canGet() {
        return INSTANCE != null;
    }

    @Override
    public void onLoad() {
        final var adapter = resolveHologramAdapter();

        if (adapter == null) {
            fancyLogger.warn("""
                    --------------------------------------------------
                    Unsupported minecraft server version.
                    Please update the server to one of (%s).
                    Disabling the FancyHolograms plugin.
                    --------------------------------------------------
                    """.formatted(String.join(" / ", ServerVersion.getSupportedVersions())));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        hologramsManager = new HologramManagerImpl(this, adapter);

        fancyLogger.info("Successfully loaded FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    @Override
    public void onEnable() {
        getHologramConfiguration().reload(this); // initialize configuration

        new FancyLib(INSTANCE); // initialize FancyLib

        if (!ServerSoftware.isPaper()) {
            fancyLogger.warn("""
                    --------------------------------------------------
                    It is recommended to use Paper as server software.
                    Because you are not using paper, the plugin
                    might not work correctly.
                    --------------------------------------------------
                    """);
        }

        LogLevel logLevel;
        try {
            logLevel = LogLevel.valueOf(getHologramConfiguration().getLogLevel());
        } catch (IllegalArgumentException e) {
            logLevel = LogLevel.INFO;
        }
        fancyLogger.setCurrentLevel(logLevel);
        IFancySitula.LOGGER.setCurrentLevel(logLevel);

        FHFeatureFlags.load();

        reloadCommands();

        registerListeners();

        versionConfig.load();
        getHologramsManager().initializeTasks();

        if (getHologramConfiguration().isAutosaveEnabled()) {
            getHologramThread().scheduleAtFixedRate(() -> {
                if (hologramsManager != null) {
                    hologramsManager.saveHolograms();
                }
            }, getHologramConfiguration().getAutosaveInterval(), getHologramConfiguration().getAutosaveInterval() * 60L, TimeUnit.SECONDS);
        }

        FHConversionRegistry.registerBuiltInConverters();

        fancyLogger.info("Successfully enabled FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        hologramsManager.saveHolograms();
        hologramThread.shutdown();
        fileStorageExecutor.shutdown();
        INSTANCE = null;

        fancyLogger.info("Successfully disabled FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    @Override
    public JavaPlugin getPlugin() {
        return INSTANCE;
    }

    @Override
    public ExtendedFancyLogger getFancyLogger() {
        return fancyLogger;
    }

    public @NotNull VersionConfig getVersionConfig() {
        return versionConfig;
    }

    @ApiStatus.Internal
    public @NotNull HologramManagerImpl getHologramsManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramManager getHologramManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramConfiguration getHologramConfiguration() {
        return configuration;
    }

    @Override
    public void setHologramConfiguration(HologramConfiguration configuration, boolean reload) {
        this.configuration = configuration;

        if (reload) {
            configuration.reload(this);
            reloadCommands();
        }
    }

    @Override
    public HologramStorage getHologramStorage() {
        return hologramStorage;
    }

    @Override
    public void setHologramStorage(HologramStorage storage, boolean reload) {
        this.hologramStorage = storage;

        if (reload) {
            getHologramsManager().reloadHolograms();
        }
    }

    public ScheduledExecutorService getHologramThread() {
        return hologramThread;
    }

    public ExecutorService getFileStorageExecutor() {
        return this.fileStorageExecutor;
    }

    private @Nullable Function<HologramData, Hologram> resolveHologramAdapter() {
        final var version = Bukkit.getMinecraftVersion();

        // check if the server version is supported by FancySitula
        if (ServerVersion.isVersionSupported(version)) {
            return HologramImpl::new;
        }

        return null;
    }

    public void reloadCommands() {
        Collection<Command> commands = Arrays.asList(new HologramCMD(this), new FancyHologramsCMD(this));

        if (getHologramConfiguration().isRegisterCommands()) {
            commands.forEach(command -> getServer().getCommandMap().register("fancyholograms", command));
        } else {
            commands.stream().filter(Command::isRegistered).forEach(command ->
                    command.unregister(getServer().getCommandMap()));
        }

        if (false) {
            FancyHologramsTestCMD fancyHologramsTestCMD = new FancyHologramsTestCMD(this);
            getServer().getCommandMap().register("fancyholograms", fancyHologramsTestCMD);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);
        if (Set.of("1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8").contains(Bukkit.getMinecraftVersion())) {
            getServer().getPluginManager().registerEvents(new PlayerLoadedListener(), this);
        }

        if (PluginUtils.isFancyNpcsEnabled()) {
            getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        }

        if (FHFeatureFlags.DISABLE_HOLOGRAMS_FOR_BEDROCK_PLAYERS.isEnabled() && PluginUtils.isFloodgateEnabled()) {
            getServer().getPluginManager().registerEvents(new BedrockPlayerListener(), this);
        }
    }

}
