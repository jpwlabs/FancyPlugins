package de.oliver.fancynpcs.security;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InteractionDispatchPolicyTest {

    @Test
    void onePhysicalRightClickProducesOneBusinessDispatch() {
        var packets = List.of(
                new Packet(EquipmentSlot.HAND, false, true),
                new Packet(EquipmentSlot.HAND, false, false),
                new Packet(EquipmentSlot.OFF_HAND, false, false));

        long dispatches = packets.stream()
                .filter(packet -> InteractionDispatchPolicy.shouldDispatch(
                        packet.hand(), packet.attack(), packet.hasHitPosition(), EntityType.PLAYER))
                .count();

        assertEquals(1, dispatches);
    }

    @Test
    void onePhysicalLeftClickProducesOneBusinessDispatch() {
        var packets = List.of(
                new Packet(EquipmentSlot.HAND, true, false),
                new Packet(EquipmentSlot.OFF_HAND, true, false));

        long dispatches = packets.stream()
                .filter(packet -> InteractionDispatchPolicy.shouldDispatch(
                        packet.hand(), packet.attack(), packet.hasHitPosition(), EntityType.PLAYER))
                .count();

        assertEquals(1, dispatches);
    }

    private record Packet(EquipmentSlot hand, boolean attack, boolean hasHitPosition) {
    }
}
