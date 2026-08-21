package com.fancyinnovations.fancydialogs.listener;

import com.fancyinnovations.fancydialogs.FancyDialogsPlugin;
import com.fancyinnovations.fancydialogs.api.Dialog;
import de.oliver.fancysitula.api.utils.FS_PacketListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.function.Supplier;

public class PlayerQuitListener implements Listener {

    private final FS_PacketListener packetListener;
    private final Supplier<Collection<Dialog>> dialogs;

    public PlayerQuitListener() {
        this(
                CustomClickActionPacketListener.get().getPacketListener(),
                () -> FancyDialogsPlugin.get().getDialogRegistry().getAll()
        );
    }

    PlayerQuitListener(FS_PacketListener packetListener, Supplier<Collection<Dialog>> dialogs) {
        this.packetListener = packetListener;
        this.dialogs = dialogs;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanUp(event.getPlayer());
    }

    void cleanUp(Player player) {
        packetListener.uninject(player);

        for (Dialog dialog : dialogs.get()) {
            dialog.removeViewer(player);
        }
    }

}
