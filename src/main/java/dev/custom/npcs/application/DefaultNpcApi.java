package dev.custom.npcs.application;

import dev.custom.npcs.api.NpcApi;
import dev.custom.npcs.api.NpcFactory;
import dev.custom.npcs.api.NpcRegistry;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;

public record DefaultNpcApi(
        NpcRegistry registry,
        NpcFactory factory,
        NpcTraitRegistry traits,
        NpcBehaviorRegistry behaviors,
        NpcInteractionRegistry interactions
) implements NpcApi {
}
