package dev.custom.npcs.api;

import dev.custom.npcs.infrastructure.NpcEntityBridge;

public interface NpcTrait {
    String key();

    NpcProfile apply(NpcProfile profile, String value);

    void applyToBridge(NpcProfile profile, NpcEntityBridge bridge);
}
