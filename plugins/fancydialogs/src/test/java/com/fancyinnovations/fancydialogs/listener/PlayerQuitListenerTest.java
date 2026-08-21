package com.fancyinnovations.fancydialogs.listener;

import de.oliver.fancysitula.api.packets.FS_ServerboundPacket;
import de.oliver.fancysitula.api.utils.FS_PacketListener;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerQuitListenerTest {

    @Test
    void quitUninjectsThePlayersPacketHandler() {
        AtomicReference<Player> uninjected = new AtomicReference<>();
        FS_PacketListener packetListener = new FS_PacketListener(FS_ServerboundPacket.Type.CUSTOM_CLICK_ACTION) {
            @Override
            public void inject(Player player) {
            }

            @Override
            public void uninject(Player player) {
                uninjected.set(player);
            }
        };
        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> null
        );

        new PlayerQuitListener(packetListener, List::of).cleanUp(player);

        assertSame(player, uninjected.get());
    }
}
