package de.oliver.fancynpcs.listeners;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.security.InteractionDispatchPolicy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerUseUnknownEntityListener implements Listener {

    @EventHandler
    public void onPlayerUseUnknownEntity(final PlayerUseUnknownEntityEvent event) {
        final Npc npc = FancyNpcs.getInstance().getNpcManagerImpl().getNpc(event.getEntityId());
        // Skipping entities that are not FancyNpcs' NPCs
        if (npc == null)
            return;
        if (InteractionDispatchPolicy.shouldDispatch(
                event.getHand(),
                event.isAttack(),
                event.getClickedRelativePosition() != null,
                npc.getData().getType())) {
            npc.interact(event.getPlayer(), event.isAttack() ? ActionTrigger.LEFT_CLICK : ActionTrigger.RIGHT_CLICK);
        }
    }

}
