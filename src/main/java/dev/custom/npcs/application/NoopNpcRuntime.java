package dev.custom.npcs.application;

import cn.nukkit.Player;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.infrastructure.NpcEntityBridge;

import java.util.Optional;

public final class NoopNpcRuntime implements NpcRuntime {
    @Override
    public NpcHandle spawn(NpcProfile profile) {
        return new RuntimeNpcHandle(profile, false, 0L);
    }

    @Override
    public NpcHandle update(NpcProfile profile) {
        return new RuntimeNpcHandle(profile, false, 0L);
    }

    @Override
    public NpcHandle despawn(String id) {
        throw new IllegalArgumentException("NPC not spawned: " + id);
    }

    @Override
    public Optional<NpcHandle> find(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<NpcHandle> findByRuntimeId(long runtimeId) {
        return Optional.empty();
    }

    @Override
    public Optional<NpcEntityBridge> bridge(String id) {
        return Optional.empty();
    }

    @Override
    public void onInteract(Player player, long runtimeId) {
    }

    @Override
    public void tick() {
    }

    @Override
    public void despawnAll() {
    }
}
