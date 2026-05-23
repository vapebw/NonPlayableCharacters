package dev.custom.npcs.presentation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcSelectionContext {
    private final Map<UUID, String> selections = new ConcurrentHashMap<>();

    public void select(UUID playerId, String npcId) {
        selections.put(playerId, npcId);
    }

    public Optional<String> selection(UUID playerId) {
        return Optional.ofNullable(selections.get(playerId));
    }
}
