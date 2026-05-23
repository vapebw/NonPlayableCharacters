package dev.custom.npcs.api;

import java.util.OptionalLong;

public interface NpcHandle {
    NpcProfile profile();

    boolean spawned();

    OptionalLong runtimeId();
}
