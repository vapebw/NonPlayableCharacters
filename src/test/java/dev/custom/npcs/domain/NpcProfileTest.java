package dev.custom.npcs.domain;

import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcVisualProfile;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NpcProfileTest {
    @Test
    public void updatesTraitsBehaviorsAndMetadataWithoutLosingExistingState() {
        NpcProfile profile = new NpcProfile(
                "guide",
                "Guide",
                new NpcLocation("spawn", 10.5, 64.0, 12.5, 180.0f, 0.0f),
                NpcVisualProfile.empty(),
                NpcFlags.defaults(),
                Map.of("visible_name", "true"),
                Set.of("idle"),
                Map.of("role", "greeter")
        );

        NpcProfile updated = profile
                .withDisplayName("Guide Alpha")
                .withLocation(new NpcLocation("hub", 1.0, 2.0, 3.0, 90.0f, 0.0f))
                .withTrait("look_at_player", "true")
                .withBehavior("click_action")
                .withMetadata("scene", "spawn");

        assertEquals("Guide Alpha", updated.displayName());
        assertEquals("hub", updated.location().world());
        assertEquals("true", updated.traits().get("visible_name"));
        assertEquals("true", updated.traits().get("look_at_player"));
        assertTrue(updated.behaviors().contains("idle"));
        assertTrue(updated.behaviors().contains("click_action"));
        assertEquals("greeter", updated.metadata().get("role"));
        assertEquals("spawn", updated.metadata().get("scene"));
    }
}
