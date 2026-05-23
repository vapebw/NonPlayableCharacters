package dev.custom.npcs.infrastructure;

import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcVisualProfile;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class YamlNpcRepositoryTest {
    @Test
    public void storesAndLoadsNpcProfilesFromYaml() throws IOException {
        Path tempDir = Files.createTempDirectory("npcs-yaml-test");
        YamlNpcRepository repository = new YamlNpcRepository(tempDir.resolve("npcs.yml").toFile());
        NpcProfile profile = new NpcProfile(
                "guide",
                "Guide",
                NpcEntityType.HUMAN,
                new NpcLocation("spawn", 10.0, 64.0, 10.0, 180.0f, 0.0f),
                new NpcVisualProfile("skin:guide", "geometry.humanoid.custom", "{\"format_version\":\"1.12.0\"}", "skin-payload", "{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}"),
                new NpcFlags(true, true, true, true, true),
                Map.of("look_at_player", "true", "visible_name", "true"),
                Set.of("idle", "click_action"),
                Map.of("scene", "spawn", "skinSource", "file")
        );

        repository.save(profile);

        List<NpcProfile> loaded = repository.loadAll();

        assertEquals(1, loaded.size());
        assertEquals("guide", loaded.get(0).id());
        assertEquals(NpcEntityType.HUMAN, loaded.get(0).entityType());
        assertEquals("skin:guide", loaded.get(0).visual().skinId());
        assertEquals("{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}", loaded.get(0).visual().skinResourcePatch());
        assertTrue(loaded.get(0).behaviors().contains("click_action"));
        assertEquals("file", loaded.get(0).metadata().get("skinSource"));
    }

    @Test
    public void storesAndLoadsMobProfilesFromYaml() throws IOException {
        Path tempDir = Files.createTempDirectory("npcs-yaml-mob-test");
        YamlNpcRepository repository = new YamlNpcRepository(tempDir.resolve("npcs.yml").toFile());
        NpcProfile profile = new NpcProfile(
                "guard",
                "Guard",
                NpcEntityType.MOB,
                new NpcLocation("spawn", 20.0, 64.0, 20.0, 0.0f, 0.0f),
                NpcVisualProfile.empty(),
                new NpcFlags(true, true, true, true, true),
                Map.of(),
                Set.of("idle"),
                Map.of("mobType", "zombie")
        );

        repository.save(profile);

        List<NpcProfile> loaded = repository.loadAll();

        assertEquals(1, loaded.size());
        assertEquals(NpcEntityType.MOB, loaded.get(0).entityType());
        assertEquals("zombie", loaded.get(0).metadata().get("mobType"));
    }
}
