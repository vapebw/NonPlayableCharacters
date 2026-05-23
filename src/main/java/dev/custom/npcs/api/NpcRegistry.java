package dev.custom.npcs.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface NpcRegistry {
    NpcHandle create(String id, String displayName, NpcLocation location);

    Optional<NpcHandle> find(String id);

    Collection<NpcHandle> all();

    boolean remove(String id);

    NpcHandle rename(String id, String displayName);

    NpcHandle move(String id, NpcLocation location);

    NpcHandle setTrait(String id, String traitKey, String value);

    NpcHandle addBehavior(String id, String behaviorKey);

    NpcHandle spawn(String id);

    NpcHandle despawn(String id);

    void saveAll();

    void loadAll();

    void spawnPersistent();

    void despawnAll();

    void tick();
}
