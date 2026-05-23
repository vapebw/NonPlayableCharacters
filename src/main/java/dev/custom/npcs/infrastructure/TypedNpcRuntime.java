package dev.custom.npcs.infrastructure;

import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcProfile;

import java.util.Optional;

public interface TypedNpcRuntime {
    NpcEntityType type();

    NpcRuntimeBridge spawn(NpcProfile profile);

    NpcRuntimeBridge update(NpcProfile profile);

    NpcRuntimeBridge despawn(String id);

    Optional<NpcRuntimeBridge> find(String id);

    Optional<NpcRuntimeBridge> findByRuntimeId(long runtimeId);

    void despawnAll();
}
