package de.oliver.fancylib;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class VersionConfig {

    private final Plugin plugin;
    private String version;
    private String commitHash;
    private String channel;
    private String platform;

    public VersionConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(new FileUtils().readResource("version.yml"));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

        version = config.getString("version", "undefined");
        commitHash = config.getString("commit_hash", "undefined");
        channel = config.getString("channel", "undefined");
        platform = config.getString("platform", "undefined");
    }

    public boolean isDevelopmentBuild() {
        return !channel.equalsIgnoreCase("release");
    }

    public void checkVersionAndDisplay(CommandSender sender, boolean displayOnlyIfOutdated) {
        if (!displayOnlyIfOutdated) {
            MessageHelper.success(sender, localVersion());
        }
    }

    private String localVersion() {
        String result = "This server is using the JPW-locked build of {plugin}.\n" +
                "Version: {version} (Git: {hash})";

        result = result.replace("{plugin}", plugin.getName())
                .replace("{version}", version)
                .replace("{hash}", commitHash.substring(0, 7));

        return result;
    }

    public String getVersion() {
        return version;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getChannel() {
        return channel;
    }

    public String getPlatform() {
        return platform;
    }
}
