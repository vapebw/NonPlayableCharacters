package dev.custom.npcs.infrastructure;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.passive.EntityNpc;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcInteraction;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.application.DefaultNpcRegistry;
import dev.custom.npcs.application.NpcRuntime;
import dev.custom.npcs.application.RuntimeNpcHandle;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PowerNukkitNpcRuntime implements NpcRuntime {
    private final PluginBase plugin;
    private final DefaultNpcRegistry registry;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final NpcInteractionRegistry interactionRegistry;
    private final Map<String, NpcEntityBridge> bridges = new LinkedHashMap<>();
    private final Map<Long, String> runtimeIndex = new LinkedHashMap<>();

    public PowerNukkitNpcRuntime(PluginBase plugin, DefaultNpcRegistry registry, NpcBehaviorRegistry behaviorRegistry, NpcInteractionRegistry interactionRegistry) {
        this.plugin = plugin;
        this.registry = registry;
        this.behaviorRegistry = behaviorRegistry;
        this.interactionRegistry = interactionRegistry;
    }

    @Override
    public NpcHandle spawn(NpcProfile profile) {
        bridges.computeIfPresent(profile.id(), (id, bridge) -> {
            bridge.despawn();
            runtimeIndex.remove(bridge.runtimeId());
            return null;
        });
        Level level = plugin.getServer().getLevelByName(profile.location().world());
        if (level == null) {
            throw new IllegalArgumentException("World not loaded: " + profile.location().world());
        }
        Location location = new Location(profile.location().x(), profile.location().y(), profile.location().z(), profile.location().yaw(), profile.location().pitch(), level);
        Entity created = Entity.createEntity(Entity.NPC, location);
        if (!(created instanceof EntityNpc npc)) {
            throw new IllegalStateException("Could not create NPC entity for " + profile.id());
        }
        NpcEntityBridge bridge = new NpcEntityBridge(npc);
        bridge.apply(profile);
        bridge.spawn();
        bridges.put(profile.id(), bridge);
        runtimeIndex.put(bridge.runtimeId(), profile.id());
        RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, true, bridge.runtimeId());
        for (String key : profile.behaviors()) {
            behaviorRegistry.require(key).onSpawn(handle, bridge);
        }
        return handle;
    }

    @Override
    public NpcHandle update(NpcProfile profile) {
        NpcEntityBridge bridge = bridges.get(profile.id());
        if (bridge == null) {
            return new RuntimeNpcHandle(profile, false, 0L);
        }
        bridge.apply(profile);
        return new RuntimeNpcHandle(profile, true, bridge.runtimeId());
    }

    @Override
    public NpcHandle despawn(String id) {
        NpcEntityBridge bridge = bridges.remove(id);
        if (bridge == null) {
            throw new IllegalArgumentException("NPC not spawned: " + id);
        }
        runtimeIndex.remove(bridge.runtimeId());
        NpcProfile profile = registry.find(id).orElseThrow().profile();
        RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, false, 0L);
        for (String key : profile.behaviors()) {
            behaviorRegistry.require(key).onDespawn(handle, bridge);
        }
        bridge.despawn();
        return handle;
    }

    @Override
    public Optional<NpcHandle> find(String id) {
        NpcEntityBridge bridge = bridges.get(id);
        if (bridge == null) {
            return Optional.empty();
        }
        return registry.find(id).map(handle -> new RuntimeNpcHandle(handle.profile(), true, bridge.runtimeId()));
    }

    @Override
    public Optional<NpcHandle> findByRuntimeId(long runtimeId) {
        String id = runtimeIndex.get(runtimeId);
        return id == null ? Optional.empty() : find(id);
    }

    @Override
    public Optional<NpcEntityBridge> bridge(String id) {
        return Optional.ofNullable(bridges.get(id));
    }

    @Override
    public void onInteract(Player player, long runtimeId) {
        String id = runtimeIndex.get(runtimeId);
        if (id == null) {
            return;
        }
        NpcProfile profile = registry.find(id).orElseThrow().profile();
        NpcEntityBridge bridge = bridges.get(id);
        RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, true, bridge.runtimeId());
        for (String key : profile.behaviors()) {
            behaviorRegistry.require(key).onInteract(handle, bridge, player);
        }
        String interactionKey = profile.metadata().get("interaction");
        if (interactionKey != null) {
            NpcInteraction interaction = interactionRegistry.find(interactionKey);
            if (interaction != null) {
                interaction.execute(handle, player);
            }
        }
    }

    @Override
    public void tick() {
        for (Map.Entry<String, NpcEntityBridge> entry : bridges.entrySet()) {
            NpcProfile profile = registry.find(entry.getKey()).orElseThrow().profile();
            RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, true, entry.getValue().runtimeId());
            for (String key : profile.behaviors()) {
                NpcBehavior behavior = behaviorRegistry.require(key);
                behavior.onTick(handle, entry.getValue());
            }
        }
    }

    @Override
    public void despawnAll() {
        for (String id : bridges.keySet().toArray(String[]::new)) {
            despawn(id);
        }
    }
}
