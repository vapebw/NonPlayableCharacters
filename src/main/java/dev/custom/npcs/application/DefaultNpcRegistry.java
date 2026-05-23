package dev.custom.npcs.application;

import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcFactory;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcRegistry;
import dev.custom.npcs.api.NpcRepository;
import dev.custom.npcs.api.NpcTrait;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DefaultNpcRegistry implements NpcRegistry {
    private final NpcRepository repository;
    private final NpcFactory factory;
    private final NpcTraitRegistry traitRegistry;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final NpcInteractionRegistry interactionRegistry;
    private final Map<String, NpcProfile> profiles = new LinkedHashMap<>();
    private NpcRuntime runtime;

    public DefaultNpcRegistry(NpcRepository repository, NpcFactory factory, NpcTraitRegistry traitRegistry, NpcBehaviorRegistry behaviorRegistry, NpcInteractionRegistry interactionRegistry, NpcRuntime runtime) {
        this.repository = repository;
        this.factory = factory;
        this.traitRegistry = traitRegistry;
        this.behaviorRegistry = behaviorRegistry;
        this.interactionRegistry = interactionRegistry;
        this.runtime = runtime;
    }

    public void attachRuntime(NpcRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public NpcHandle create(String id, String displayName, NpcLocation location) {
        requireAbsent(id);
        NpcProfile profile = factory.createProfile(id, displayName, location);
        profiles.put(id, profile);
        persist(profile);
        return new RuntimeNpcHandle(profile, false, 0L);
    }

    @Override
    public Optional<NpcHandle> find(String id) {
        Optional<NpcHandle> runtimeHandle = runtime.find(id);
        if (runtimeHandle.isPresent()) {
            return runtimeHandle;
        }
        NpcProfile profile = profiles.get(id);
        return profile == null ? Optional.empty() : Optional.of(new RuntimeNpcHandle(profile, false, 0L));
    }

    @Override
    public Collection<NpcHandle> all() {
        return profiles.keySet().stream().map(id -> find(id).orElseThrow()).toList();
    }

    @Override
    public boolean remove(String id) {
        NpcProfile removed = profiles.remove(id);
        if (removed == null) {
            return false;
        }
        runtime.find(id).ifPresent(handle -> runtime.despawn(id));
        delete(id);
        return true;
    }

    @Override
    public NpcHandle rename(String id, String displayName) {
        return updateProfile(id, profile -> profile.withDisplayName(displayName));
    }

    @Override
    public NpcHandle move(String id, NpcLocation location) {
        return updateProfile(id, profile -> profile.withLocation(location));
    }

    @Override
    public NpcHandle setTrait(String id, String traitKey, String value) {
        NpcTrait trait = traitRegistry.require(traitKey);
        return updateProfile(id, profile -> trait.apply(profile, value));
    }

    @Override
    public NpcHandle addBehavior(String id, String behaviorKey) {
        NpcBehavior behavior = behaviorRegistry.require(behaviorKey);
        return updateProfile(id, profile -> profile.withBehavior(behavior.key()));
    }

    @Override
    public NpcHandle spawn(String id) {
        NpcProfile profile = requireProfile(id);
        NpcHandle handle = runtime.spawn(profile);
        profiles.put(id, handle.profile());
        return handle;
    }

    @Override
    public NpcHandle despawn(String id) {
        runtime.find(id).orElseThrow(() -> new IllegalArgumentException("NPC not spawned: " + id));
        return runtime.despawn(id);
    }

    @Override
    public void saveAll() {
        profiles.values().forEach(this::persist);
    }

    @Override
    public void loadAll() {
        profiles.clear();
        try {
            repository.loadAll().forEach(profile -> profiles.put(profile.id(), profile));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void spawnPersistent() {
        profiles.keySet().forEach(this::spawn);
    }

    @Override
    public void despawnAll() {
        runtime.despawnAll();
    }

    @Override
    public void tick() {
        runtime.tick();
    }

    public Optional<NpcHandle> findByRuntimeId(long runtimeId) {
        return runtime.findByRuntimeId(runtimeId);
    }

    public void onInteract(cn.nukkit.Player player, long runtimeId) {
        runtime.onInteract(player, runtimeId);
    }

    private NpcHandle updateProfile(String id, java.util.function.UnaryOperator<NpcProfile> updater) {
        NpcProfile current = requireProfile(id);
        NpcProfile updated = updater.apply(current);
        profiles.put(id, updated);
        persist(updated);
        return runtime.find(id).isPresent() ? runtime.update(updated) : new RuntimeNpcHandle(updated, false, 0L);
    }

    private NpcProfile requireProfile(String id) {
        NpcProfile profile = profiles.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown NPC: " + id);
        }
        return profile;
    }

    private void requireAbsent(String id) {
        if (profiles.containsKey(id)) {
            throw new IllegalArgumentException("NPC already exists: " + id);
        }
    }

    private void persist(NpcProfile profile) {
        try {
            repository.save(profile);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void delete(String id) {
        try {
            repository.delete(id);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
