package dev.custom.npcs.application;

import dev.custom.npcs.api.NpcFactory;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcVisualProfile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public final class DefaultNpcFactory implements NpcFactory {
    @Override
    public NpcProfile createProfile(String id, String displayName, NpcLocation location) {
        return new NpcProfile(id, displayName, NpcEntityType.NPC, location, NpcVisualProfile.empty(), NpcFlags.defaults(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashMap<>());
    }

    @Override
    public NpcProfile withType(NpcProfile profile, NpcEntityType entityType) {
        return profile.withEntityType(entityType);
    }
}
