package dev.custom.npcs.application;

import cn.nukkit.Player;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.infrastructure.NpcRuntimeBridge;

import java.util.Optional;

public interface NpcRuntime {
    NpcHandle spawn(NpcProfile profile);

    NpcHandle update(NpcProfile profile);

    NpcHandle despawn(String id);

    Optional<NpcHandle> find(String id);

    Optional<NpcHandle> findByRuntimeId(long runtimeId);

    Optional<NpcRuntimeBridge> bridge(String id);

    void onInteract(Player player, long runtimeId);

    void tick();

    void despawnAll();
}
