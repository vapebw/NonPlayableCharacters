package dev.custom.npcs.domain;

import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcTrait;
import dev.custom.npcs.infrastructure.NpcRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcTraitRegistry {
    private final Map<String, NpcTrait> traits = new LinkedHashMap<>();

    public NpcTraitRegistry() {
        register(new FlagTrait("visible_name") {
            @Override
            protected NpcFlags updateFlags(NpcFlags flags, boolean value) {
                return flags.withNameVisible(value);
            }
        });
        register(new FlagTrait("look_at_player") {
            @Override
            protected NpcFlags updateFlags(NpcFlags flags, boolean value) {
                return flags.withLookAtPlayer(value);
            }
        });
        register(new FlagTrait("immobile") {
            @Override
            protected NpcFlags updateFlags(NpcFlags flags, boolean value) {
                return flags.withImmobile(value);
            }
        });
        register(new FlagTrait("protected") {
            @Override
            protected NpcFlags updateFlags(NpcFlags flags, boolean value) {
                return flags.withProtectedFromDamage(value);
            }
        });
        register(new FlagTrait("push_protected") {
            @Override
            protected NpcFlags updateFlags(NpcFlags flags, boolean value) {
                return flags.withPushProtected(value);
            }
        });
    }

    public void register(NpcTrait trait) {
        traits.put(trait.key(), trait);
    }

    public NpcTrait require(String key) {
        NpcTrait trait = traits.get(key);
        if (trait == null) {
            throw new IllegalArgumentException("Unknown trait: " + key);
        }
        return trait;
    }

    public Iterable<NpcTrait> all() {
        return traits.values();
    }

    private abstract static class FlagTrait implements NpcTrait {
        private final String key;

        private FlagTrait(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public NpcProfile apply(NpcProfile profile, String value) {
            boolean parsed = Boolean.parseBoolean(value);
            return profile.withFlags(updateFlags(profile.flags(), parsed)).withTrait(key, value);
        }

        @Override
        public void applyToBridge(NpcProfile profile, NpcRuntimeBridge bridge) {
            bridge.apply(profile);
        }

        protected abstract NpcFlags updateFlags(NpcFlags flags, boolean value);
    }
}
