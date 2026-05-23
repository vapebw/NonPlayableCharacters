package dev.custom.npcs.application;

import dev.custom.npcs.api.NpcFactory;
import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcRepository;
import dev.custom.npcs.api.NpcVisualProfile;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultNpcRegistryTest {
    @Test
    public void createsAndPersistsNpcProfilesWithStableIds() throws IOException {
        InMemoryRepository repository = new InMemoryRepository();
        DefaultNpcRegistry registry = new DefaultNpcRegistry(
                repository,
                new SimpleFactory(),
                new NpcTraitRegistry(),
                new NpcBehaviorRegistry(),
                new NpcInteractionRegistry(),
                new NoopNpcRuntime()
        );

        registry.create("guide", "Guide", new NpcLocation("spawn", 1.0, 65.0, 1.0, 0.0f, 0.0f));
        registry.rename("guide", "Guide Prime");
        registry.changeType("guide", NpcEntityType.HUMAN);
        registry.updateVisual("guide", new NpcVisualProfile("skin:guide", "geometry.humanoid.custom", "geometry", "skin", "patch"));
        registry.setMetadata("guide", "skinSource", "player");
        registry.setTrait("guide", "look_at_player", "true");
        registry.addBehavior("guide", "click_action");
        registry.saveAll();

        assertEquals(1, repository.saved.size());
        NpcProfile profile = repository.saved.get("guide");
        assertEquals("Guide Prime", profile.displayName());
        assertEquals(NpcEntityType.HUMAN, profile.entityType());
        assertEquals("skin:guide", profile.visual().skinId());
        assertEquals("player", profile.metadata().get("skinSource"));
        assertEquals("true", profile.traits().get("look_at_player"));
        assertTrue(profile.behaviors().contains("click_action"));
    }

    @Test
    public void rejectsDuplicateNpcIds() {
        InMemoryRepository repository = new InMemoryRepository();
        DefaultNpcRegistry registry = new DefaultNpcRegistry(
                repository,
                new SimpleFactory(),
                new NpcTraitRegistry(),
                new NpcBehaviorRegistry(),
                new NpcInteractionRegistry(),
                new NoopNpcRuntime()
        );

        registry.create("guide", "Guide", new NpcLocation("spawn", 1.0, 65.0, 1.0, 0.0f, 0.0f));

        try {
            registry.create("guide", "Guide Again", new NpcLocation("spawn", 2.0, 65.0, 2.0, 0.0f, 0.0f));
            fail("Expected duplicate id rejection");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void returnsStoredProfileWhenRuntimeCannotResolveIt() {
        InMemoryRepository repository = new InMemoryRepository();
        DefaultNpcRegistry registry = new DefaultNpcRegistry(
                repository,
                new SimpleFactory(),
                new NpcTraitRegistry(),
                new NpcBehaviorRegistry(),
                new NpcInteractionRegistry(),
                new NoopNpcRuntime()
        );

        registry.create("guide", "Guide", new NpcLocation("spawn", 1.0, 65.0, 1.0, 0.0f, 0.0f));

        NpcProfile profile = registry.profile("guide");

        assertEquals("guide", profile.id());
        assertEquals("Guide", profile.displayName());
    }

    private static final class SimpleFactory implements NpcFactory {
        @Override
        public NpcProfile createProfile(String id, String displayName, NpcLocation location) {
            return new NpcProfile(id, displayName, NpcEntityType.NPC, location, NpcVisualProfile.empty(), NpcFlags.defaults(), Map.of(), Set.of(), Map.of());
        }

        @Override
        public NpcProfile withType(NpcProfile profile, NpcEntityType entityType) {
            return profile.withEntityType(entityType);
        }
    }

    private static final class InMemoryRepository implements NpcRepository {
        private final Map<String, NpcProfile> saved = new LinkedHashMap<>();

        @Override
        public List<NpcProfile> loadAll() {
            return List.copyOf(saved.values());
        }

        @Override
        public void save(NpcProfile profile) {
            saved.put(profile.id(), profile);
        }

        @Override
        public void delete(String id) {
            saved.remove(id);
        }

        @Override
        public Optional<NpcProfile> find(String id) {
            return Optional.ofNullable(saved.get(id));
        }
    }
}
