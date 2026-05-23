package dev.custom.npcs.infrastructure;

import cn.nukkit.utils.Config;
import dev.custom.npcs.api.NpcFlags;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.api.NpcProfile;
import dev.custom.npcs.api.NpcRepository;
import dev.custom.npcs.api.NpcVisualProfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class YamlNpcRepository implements NpcRepository {
    private final File file;

    public YamlNpcRepository(File file) {
        this.file = file;
    }

    @Override
    public List<NpcProfile> loadAll() throws IOException {
        ensureFile();
        Config config = new Config(file, Config.YAML);
        Object root = config.get("npcs");
        if (!(root instanceof Map<?, ?> nodes)) {
            return List.of();
        }
        List<NpcProfile> profiles = new ArrayList<>();
        for (Map.Entry<?, ?> entry : nodes.entrySet()) {
            if (entry.getKey() instanceof String id && entry.getValue() instanceof Map<?, ?> rawNode) {
                profiles.add(readProfile(id, cast(rawNode)));
            }
        }
        return profiles;
    }

    @Override
    public void save(NpcProfile profile) throws IOException {
        ensureFile();
        Config config = new Config(file, Config.YAML);
        Map<String, Object> root = map(config.get("npcs"));
        root.put(profile.id(), writeProfile(profile));
        config.set("npcs", root);
        config.save();
    }

    @Override
    public void delete(String id) throws IOException {
        ensureFile();
        Config config = new Config(file, Config.YAML);
        Map<String, Object> root = map(config.get("npcs"));
        root.remove(id);
        config.set("npcs", root);
        config.save();
    }

    @Override
    public Optional<NpcProfile> find(String id) throws IOException {
        return loadAll().stream().filter(profile -> profile.id().equals(id)).findFirst();
    }

    private NpcProfile readProfile(String id, Map<String, Object> node) {
        Map<String, Object> location = map(node.get("location"));
        Map<String, Object> visual = map(node.get("visual"));
        Map<String, Object> flags = map(node.get("flags"));
        Map<String, String> traits = stringMap(node.get("traits"));
        Map<String, String> metadata = stringMap(node.get("metadata"));
        Set<String> behaviors = stringSet(node.get("behaviors"));
        return new NpcProfile(
                id,
                string(node.get("displayName")),
                entityType(node.get("entityType")),
                new NpcLocation(
                        string(location.get("world")),
                        number(location.get("x")),
                        number(location.get("y")),
                        number(location.get("z")),
                        (float) number(location.get("yaw")),
                        (float) number(location.get("pitch"))
                ),
                new NpcVisualProfile(
                        string(visual.get("skinId")),
                        string(visual.get("geometryName")),
                        string(visual.get("geometryData")),
                        string(visual.get("skinData")),
                        string(visual.get("skinResourcePatch"))
                ),
                new NpcFlags(
                        bool(flags.get("immobile")),
                        bool(flags.get("lookAtPlayer")),
                        bool(flags.get("nameVisible")),
                        bool(flags.get("protectedFromDamage")),
                        bool(flags.get("pushProtected"))
                ),
                traits,
                behaviors,
                metadata
        );
    }

    private Map<String, Object> writeProfile(NpcProfile profile) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("displayName", profile.displayName());
        node.put("entityType", profile.entityType().id());
        node.put("location", Map.of(
                "world", profile.location().world(),
                "x", profile.location().x(),
                "y", profile.location().y(),
                "z", profile.location().z(),
                "yaw", profile.location().yaw(),
                "pitch", profile.location().pitch()
        ));
        node.put("visual", Map.of(
                "skinId", profile.visual().skinId(),
                "geometryName", profile.visual().geometryName(),
                "geometryData", profile.visual().geometryData(),
                "skinData", profile.visual().skinData(),
                "skinResourcePatch", profile.visual().skinResourcePatch()
        ));
        node.put("flags", Map.of(
                "immobile", profile.flags().immobile(),
                "lookAtPlayer", profile.flags().lookAtPlayer(),
                "nameVisible", profile.flags().nameVisible(),
                "protectedFromDamage", profile.flags().protectedFromDamage(),
                "pushProtected", profile.flags().pushProtected()
        ));
        node.put("traits", new LinkedHashMap<>(profile.traits()));
        node.put("behaviors", new ArrayList<>(profile.behaviors()));
        node.put("metadata", new LinkedHashMap<>(profile.metadata()));
        return node;
    }

    private void ensureFile() throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return cast(raw);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, String> stringMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> raw) {
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return result;
    }

    private Set<String> stringSet(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                if (element != null) {
                    result.add(String.valueOf(element));
                }
            }
        }
        return result;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0d;
    }

    private boolean bool(Object value) {
        return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value));
    }

    private NpcEntityType entityType(Object value) {
        String raw = string(value);
        return raw.isEmpty() ? NpcEntityType.NPC : NpcEntityType.parse(raw);
    }

    private Map<String, Object> cast(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }
}
