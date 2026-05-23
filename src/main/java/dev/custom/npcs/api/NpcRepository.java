package dev.custom.npcs.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface NpcRepository {
    List<NpcProfile> loadAll() throws IOException;

    void save(NpcProfile profile) throws IOException;

    void delete(String id) throws IOException;

    Optional<NpcProfile> find(String id) throws IOException;
}
