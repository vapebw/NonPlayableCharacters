package dev.custom.npcs.application;

import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcProfile;

import java.util.OptionalLong;

public final class RuntimeNpcHandle implements NpcHandle {
    private final NpcProfile profile;
    private final boolean spawned;
    private final long runtimeId;

    public RuntimeNpcHandle(NpcProfile profile, boolean spawned, long runtimeId) {
        this.profile = profile;
        this.spawned = spawned;
        this.runtimeId = runtimeId;
    }

    @Override
    public NpcProfile profile() {
        return profile;
    }

    @Override
    public boolean spawned() {
        return spawned;
    }

    @Override
    public OptionalLong runtimeId() {
        return spawned ? OptionalLong.of(runtimeId) : OptionalLong.empty();
    }
}
