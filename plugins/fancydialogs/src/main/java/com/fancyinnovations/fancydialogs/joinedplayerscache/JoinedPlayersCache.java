package com.fancyinnovations.fancydialogs.joinedplayerscache;

import com.fancyinnovations.fancydialogs.FancyDialogsPlugin;
import de.oliver.jpw.logging.properties.ThrowableProperty;
import de.oliver.jdb.JDB;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JoinedPlayersCache {

    private final Set<String> playersJoined;
    private final JDB jdb;

    public JoinedPlayersCache() {
        this.playersJoined = new HashSet<>();
        this.jdb = new JDB("plugins/FancyDialogs/data");
    }

    public void load() {
        try {
            playersJoined.clear();
            String[] data = jdb.get("joined_players", String[].class);
            if (data != null) {
                Collections.addAll(playersJoined, data);
                FancyDialogsPlugin.get().getFancyLogger().debug("Loaded joined players from file.");
            }
        } catch (Exception e) {
            FancyDialogsPlugin.get().getFancyLogger().error("Failed to load joined players cache", ThrowableProperty.of(e));
        }
    }

    public void save() {
        try {
            jdb.set("joined_players", playersJoined);
            FancyDialogsPlugin.get().getFancyLogger().debug("Saved joined players cache");
        } catch (Exception e) {
            FancyDialogsPlugin.get().getFancyLogger().error("Failed to save joined players cache", ThrowableProperty.of(e));
        }
    }

    public void clear() {
        playersJoined.clear();
        save();
    }

    public boolean checkIfPlayerJoined(UUID playerUUID) {
        return playersJoined.contains(playerUUID.toString());
    }

    public void addPlayer(UUID playerUUID) {
        if (playersJoined.add(playerUUID.toString())) {
            save();
        }
    }

}
