package dev.custom.npcs.presentation;

import cn.nukkit.entity.passive.EntityNpc;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import dev.custom.npcs.application.DefaultNpcRegistry;

public final class NpcInteractionListener implements Listener {
    private final DefaultNpcRegistry registry;
    private final NpcSelectionContext selectionContext;

    public NpcInteractionListener(DefaultNpcRegistry registry, NpcSelectionContext selectionContext) {
        this.registry = registry;
        this.selectionContext = selectionContext;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getEntity() instanceof EntityNpc npc)) {
            return;
        }
        registry.findByRuntimeId(npc.getId()).ifPresent(handle -> {
            selectionContext.select(event.getPlayer().getUniqueId(), handle.profile().id());
            registry.onInteract(event.getPlayer(), npc.getId());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EntityNpc npc)) {
            return;
        }
        registry.findByRuntimeId(npc.getId()).ifPresent(handle -> {
            if (handle.profile().flags().protectedFromDamage()) {
                event.setCancelled();
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EntityNpc npc)) {
            return;
        }
        registry.findByRuntimeId(npc.getId()).ifPresent(handle -> {
            if (handle.profile().flags().pushProtected()) {
                event.setKnockBack(0);
                event.setCancelled();
            }
        });
    }
}
