package dev.custom.npcs.infrastructure;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MobEntityRuntime implements TypedNpcRuntime {
    private final PluginBase plugin;
    private final Map<String, MobEntityBridge> bridges = new LinkedHashMap<>();
    private final Map<Long, String> runtimeIndex = new LinkedHashMap<>();

    public MobEntityRuntime(PluginBase plugin) {
        this.plugin = plugin;
    }

    @Override
    public NpcEntityType type() {
        return NpcEntityType.MOB;
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
        String mobType = profile.metadata().getOrDefault("mobType", MobCatalog.defaultMobType());
        String identifier = MobCatalog.identifier(mobType);
        Location location = new Location(profile.location().x(), profile.location().y(), profile.location().z(), profile.location().yaw(), profile.location().pitch(), level);
        Entity created = Entity.createEntity(identifier, location);
        if (created == null) {
            throw new IllegalStateException("Could not create mob entity for " + profile.id() + " using " + identifier);
        }
        MobEntityBridge bridge = new MobEntityBridge(created);
        bridge.apply(profile);
        bridge.spawn();
        bridges.put(profile.id(), bridge);
        runtimeIndex.put(bridge.runtimeId(), profile.id());
        return bridge;
    }

    @Override
    public NpcRuntimeBridge update(NpcProfile profile) {
        MobEntityBridge bridge = bridges.get(profile.id());
        if (bridge == null) {
            throw new IllegalArgumentException("NPC not spawned: " + profile.id());
        }
        bridge.apply(profile);
        return bridge;
    }

    @Override
    public NpcRuntimeBridge despawn(String id) {
        MobEntityBridge bridge = bridges.remove(id);
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
