package de.oliver.fancynpcs.security;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

/**
 * The packet-to-business-action seam for physical NPC interactions.
 *
 * <p>Paper can report a single right click more than once: main hand with a hit position,
 * main hand without one, and optionally off hand. Only the canonical main-hand report is
 * allowed to cross into {@code Npc#interact}.</p>
 */
public final class InteractionDispatchPolicy {

    private InteractionDispatchPolicy() {
    }

    public static boolean shouldDispatch(
            EquipmentSlot hand,
            boolean attack,
            boolean hasHitPosition,
            EntityType entityType
    ) {
        if (hand != EquipmentSlot.HAND) {
            return false;
        }

        return attack || !hasHitPosition || Objects.requireNonNull(entityType, "entityType") == EntityType.ARMOR_STAND;
    }
}
