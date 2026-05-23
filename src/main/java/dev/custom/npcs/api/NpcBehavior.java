package dev.custom.npcs.api;

import cn.nukkit.Player;
import dev.custom.npcs.infrastructure.NpcRuntimeBridge;

public interface NpcBehavior {
    String key();

    default void onSpawn(NpcHandle handle, NpcRuntimeBridge bridge) {
    }

    default void onDespawn(NpcHandle handle, NpcRuntimeBridge bridge) {
    }

    default void onInteract(NpcHandle handle, NpcRuntimeBridge bridge, Player player) {
    }

    default void onTick(NpcHandle handle, NpcRuntimeBridge bridge) {
    }
}
