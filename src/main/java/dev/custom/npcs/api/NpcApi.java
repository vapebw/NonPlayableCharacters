package dev.custom.npcs.api;

import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;

public interface NpcApi {
    NpcRegistry registry();

    NpcFactory factory();

    NpcTraitRegistry traits();

    NpcBehaviorRegistry behaviors();

    NpcInteractionRegistry interactions();
}
