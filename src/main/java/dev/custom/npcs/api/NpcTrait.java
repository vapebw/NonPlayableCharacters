package dev.custom.npcs.api;

import dev.custom.npcs.infrastructure.NpcRuntimeBridge;

public interface NpcTrait {
    String key();

    NpcProfile apply(NpcProfile profile, String value);

    void applyToBridge(NpcProfile profile, NpcRuntimeBridge bridge);
}
