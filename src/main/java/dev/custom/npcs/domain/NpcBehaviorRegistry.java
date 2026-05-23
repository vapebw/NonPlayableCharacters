package dev.custom.npcs.domain;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.infrastructure.NpcEntityBridge;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcBehaviorRegistry {
    private final Map<String, NpcBehavior> behaviors = new LinkedHashMap<>();

    public NpcBehaviorRegistry() {
        register(new IdleBehavior());
        register(new LookAtPlayerBehavior());
        register(new ClickActionBehavior());
    }

    public void register(NpcBehavior behavior) {
        behaviors.put(behavior.key(), behavior);
    }

    public NpcBehavior require(String key) {
        NpcBehavior behavior = behaviors.get(key);
        if (behavior == null) {
            throw new IllegalArgumentException("Unknown behavior: " + key);
        }
        return behavior;
    }

    public Iterable<NpcBehavior> all() {
        return behaviors.values();
    }

    private static final class IdleBehavior implements NpcBehavior {
        @Override
        public String key() {
            return "idle";
        }
    }

    private static final class LookAtPlayerBehavior implements NpcBehavior {
        @Override
        public String key() {
            return "look_at_player";
        }

        @Override
        public void onTick(NpcHandle handle, NpcEntityBridge bridge) {
            Player nearest = bridge.nearestPlayer(8.0);
            if (nearest == null) {
                return;
            }
            Location location = bridge.location();
            location.setYawFacing(nearest);
            bridge.rotate(location.getYaw(), location.getPitch());
        }
    }

    private static final class ClickActionBehavior implements NpcBehavior {
        @Override
        public String key() {
            return "click_action";
        }
    }
}
