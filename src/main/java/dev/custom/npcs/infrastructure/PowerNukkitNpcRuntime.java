package dev.custom.npcs.infrastructure;

import cn.nukkit.Player;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcEntityType;
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
    private final DefaultNpcRegistry registry;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final NpcInteractionRegistry interactionRegistry;
    private final Map<NpcEntityType, TypedNpcRuntime> runtimes = new LinkedHashMap<>();
    private final Map<String, NpcEntityType> activeTypes = new LinkedHashMap<>();
    private final Map<Long, String> runtimeIndex = new LinkedHashMap<>();

    public PowerNukkitNpcRuntime(PluginBase plugin, DefaultNpcRegistry registry, NpcBehaviorRegistry behaviorRegistry, NpcInteractionRegistry interactionRegistry) {
        this.registry = registry;
        this.behaviorRegistry = behaviorRegistry;
        this.interactionRegistry = interactionRegistry;
        register(new NpcEntityRuntime(plugin));
        register(new HumanEntityRuntime(plugin));
        register(new MobEntityRuntime(plugin));
    }

    @Override
    public NpcHandle spawn(NpcProfile profile) {
        find(profile.id()).ifPresent(handle -> despawn(profile.id()));
        TypedNpcRuntime runtime = runtime(profile.entityType());
        NpcRuntimeBridge bridge = runtime.spawn(profile);
        activeTypes.put(profile.id(), profile.entityType());
        runtimeIndex.put(bridge.runtimeId(), profile.id());
        RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, true, bridge.runtimeId());
        for (String key : profile.behaviors()) {
            behaviorRegistry.require(key).onSpawn(handle, bridge);
        }
        return handle;
    }

    @Override
    public NpcHandle update(NpcProfile profile) {
        TypedNpcRuntime runtime = activeRuntime(profile.id()).orElse(null);
        if (runtime == null) {
            return new RuntimeNpcHandle(profile, false, 0L);
        }
        if (runtime.type() != profile.entityType()) {
            despawn(profile.id());
            return spawn(profile);
        }
        NpcRuntimeBridge bridge = runtime.update(profile);
        bridge.apply(profile);
        return new RuntimeNpcHandle(profile, true, bridge.runtimeId());
    }

    @Override
    public NpcHandle despawn(String id) {
        TypedNpcRuntime runtime = activeRuntime(id).orElse(null);
        if (runtime == null) {
            throw new IllegalArgumentException("NPC not spawned: " + id);
        }
        NpcRuntimeBridge bridge = runtime.despawn(id);
        activeTypes.remove(id);
        runtimeIndex.remove(bridge.runtimeId());
        NpcProfile profile = registry.profile(id);
        RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, false, 0L);
        for (String key : profile.behaviors()) {
            behaviorRegistry.require(key).onDespawn(handle, bridge);
        }
        bridge.despawn();
        return handle;
    }

    @Override
    public Optional<NpcHandle> find(String id) {
        Optional<TypedNpcRuntime> runtime = activeRuntime(id);
        if (runtime.isEmpty()) {
            return Optional.empty();
        }
        return runtime.get().find(id).map(bridge -> new RuntimeNpcHandle(registry.profile(id), true, bridge.runtimeId()));
    }

    @Override
    public Optional<NpcHandle> findByRuntimeId(long runtimeId) {
        String id = runtimeIndex.get(runtimeId);
        return id == null ? Optional.empty() : find(id);
    }

    @Override
    public Optional<NpcRuntimeBridge> bridge(String id) {
        return activeRuntime(id).flatMap(runtime -> runtime.find(id));
    }

    @Override
    public void onInteract(Player player, long runtimeId) {
        String id = runtimeIndex.get(runtimeId);
        if (id == null) {
            return;
        }
        NpcProfile profile = registry.profile(id);
        NpcRuntimeBridge bridge = bridge(id).orElse(null);
        if (bridge == null) {
            return;
        }
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
        for (Map.Entry<String, NpcEntityType> entry : activeTypes.entrySet()) {
            NpcProfile profile = registry.profile(entry.getKey());
            NpcRuntimeBridge bridge = runtime(entry.getValue()).find(entry.getKey()).orElse(null);
            if (bridge == null) {
                continue;
            }
            RuntimeNpcHandle handle = new RuntimeNpcHandle(profile, true, bridge.runtimeId());
            for (String key : profile.behaviors()) {
                NpcBehavior behavior = behaviorRegistry.require(key);
                behavior.onTick(handle, bridge);
            }
        }
    }

    @Override
    public void despawnAll() {
        for (String id : activeTypes.keySet().toArray(String[]::new)) {
            despawn(id);
        }
    }

    private void register(TypedNpcRuntime runtime) {
        runtimes.put(runtime.type(), runtime);
    }

    private TypedNpcRuntime runtime(NpcEntityType entityType) {
        TypedNpcRuntime runtime = runtimes.get(entityType);
        if (runtime == null) {
            throw new IllegalArgumentException("Unsupported NPC entity type: " + entityType.id());
        }
        return runtime;
    }

    private Optional<TypedNpcRuntime> activeRuntime(String id) {
        NpcEntityType entityType = activeTypes.get(id);
        return entityType == null ? Optional.empty() : Optional.of(runtime(entityType));
    }
}
