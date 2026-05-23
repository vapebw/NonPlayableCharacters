package dev.custom.npcs.domain;

import dev.custom.npcs.api.NpcInteraction;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcInteractionRegistry {
    private final Map<String, NpcInteraction> interactions = new LinkedHashMap<>();

    public void register(NpcInteraction interaction) {
        interactions.put(interaction.key(), interaction);
    }

    public NpcInteraction find(String key) {
        return interactions.get(key);
    }
}
