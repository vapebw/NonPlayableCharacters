package dev.custom.npcs.api;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record NpcProfile(
        String id,
        String displayName,
        NpcLocation location,
        NpcVisualProfile visual,
        NpcFlags flags,
        Map<String, String> traits,
        Set<String> behaviors,
        Map<String, String> metadata
) {
    public NpcProfile {
        traits = Map.copyOf(traits);
        behaviors = Set.copyOf(behaviors);
        metadata = Map.copyOf(metadata);
    }

    public NpcProfile withDisplayName(String value) {
        return new NpcProfile(id, value, location, visual, flags, traits, behaviors, metadata);
    }

    public NpcProfile withLocation(NpcLocation value) {
        return new NpcProfile(id, displayName, value, visual, flags, traits, behaviors, metadata);
    }

    public NpcProfile withVisual(NpcVisualProfile value) {
        return new NpcProfile(id, displayName, location, value, flags, traits, behaviors, metadata);
    }

    public NpcProfile withFlags(NpcFlags value) {
        return new NpcProfile(id, displayName, location, visual, value, traits, behaviors, metadata);
    }

    public NpcProfile withTrait(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(traits);
        next.put(key, value);
        return new NpcProfile(id, displayName, location, visual, flags, next, behaviors, metadata);
    }

    public NpcProfile withBehavior(String key) {
        Set<String> next = new LinkedHashSet<>(behaviors);
        next.add(key);
        return new NpcProfile(id, displayName, location, visual, flags, traits, next, metadata);
    }

    public NpcProfile withMetadata(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(metadata);
        next.put(key, value);
        return new NpcProfile(id, displayName, location, visual, flags, traits, behaviors, next);
    }
}
