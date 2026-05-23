package dev.custom.npcs.infrastructure;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HumanEntityRuntime implements TypedNpcRuntime {
    private final PluginBase plugin;
    private final Map<String, HumanEntityBridge> bridges = new LinkedHashMap<>();
    private final Map<Long, String> runtimeIndex = new LinkedHashMap<>();

    public HumanEntityRuntime(PluginBase plugin) {
        this.plugin = plugin;
    }

    @Override
    public NpcEntityType type() {
        return NpcEntityType.HUMAN;
    }

    @Override
    public NpcRuntimeBridge spawn(NpcProfile profile) {
        bridges.computeIfPresent(profile.id(), (id, bridge) -> {
            runtimeIndex.remove(bridge.runtimeId());
            bridge.despawn();
            return null;
        });
        Level level = plugin.getServer().getLevelByName(profile.location().world());
        if (level == null) {
            throw new IllegalArgumentException("World not loaded: " + profile.location().world());
        }
        Location location = new Location(profile.location().x(), profile.location().y(), profile.location().z(), profile.location().yaw(), profile.location().pitch(), level);
        CompoundTag nbt = Entity.getDefaultNBT(location, null, profile.location().yaw(), profile.location().pitch());
        nbt.putString("CustomName", profile.displayName());
        EntityHuman human = new EntityHuman(level.getChunk(location.getFloorX() >> 4, location.getFloorZ() >> 4, true), nbt);
        human.setUniqueId(UUID.nameUUIDFromBytes(("npcs:" + profile.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        HumanEntityBridge bridge = new HumanEntityBridge(human);
        bridge.apply(profile);
        bridge.spawn();
        bridges.put(profile.id(), bridge);
        runtimeIndex.put(bridge.runtimeId(), profile.id());
        return bridge;
    }

    @Override
    public NpcRuntimeBridge update(NpcProfile profile) {
        HumanEntityBridge bridge = bridges.get(profile.id());
        if (bridge == null) {
            throw new IllegalArgumentException("NPC not spawned: " + profile.id());
        }
        bridge.apply(profile);
        return bridge;
    }

    @Override
    public NpcRuntimeBridge despawn(String id) {
        HumanEntityBridge bridge = bridges.remove(id);
        if (bridge == null) {
            throw new IllegalArgumentException("NPC not spawned: " + id);
        }
        runtimeIndex.remove(bridge.runtimeId());
        bridge.despawn();
        return bridge;
    }

    @Override
    public Optional<NpcRuntimeBridge> find(String id) {
        return Optional.ofNullable(bridges.get(id));
    }

    @Override
    public Optional<NpcRuntimeBridge> findByRuntimeId(long runtimeId) {
        String id = runtimeIndex.get(runtimeId);
        return id == null ? Optional.empty() : find(id);
    }

    @Override
    public void despawnAll() {
        for (String id : bridges.keySet().toArray(String[]::new)) {
            despawn(id);
        }
    }
}
