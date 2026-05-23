package dev.custom.npcs.api;

public interface NpcFactory {
    NpcProfile createProfile(String id, String displayName, NpcLocation location);

    NpcProfile withType(NpcProfile profile, NpcEntityType entityType);
}
