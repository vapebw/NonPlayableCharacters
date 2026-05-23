package dev.custom.npcs.api;

import cn.nukkit.Player;
import dev.custom.npcs.infrastructure.NpcEntityBridge;

public interface NpcBehavior {
    String key();

    default void onSpawn(NpcHandle handle, NpcEntityBridge bridge) {
    }

    default void onDespawn(NpcHandle handle, NpcEntityBridge bridge) {
    }

    default void onInteract(NpcHandle handle, NpcEntityBridge bridge, Player player) {
    }

    default void onTick(NpcHandle handle, NpcEntityBridge bridge) {
    }
}
