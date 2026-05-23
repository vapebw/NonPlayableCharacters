package dev.custom.npcs.infrastructure;

import java.util.List;
import java.util.Map;

public final class MobCatalog {
    private static final Map<String, String> SUPPORTED = Map.of(
            "zombie", "minecraft:zombie",
            "skeleton", "minecraft:skeleton",
            "creeper", "minecraft:creeper",
            "spider", "minecraft:spider",
            "cow", "minecraft:cow",
            "pig", "minecraft:pig"
    );

    private MobCatalog() {
    }

    public static String defaultMobType() {
        return "zombie";
    }

    public static String identifier(String type) {
        String identifier = SUPPORTED.get(type.toLowerCase());
        if (identifier == null) {
            throw new IllegalArgumentException("Unsupported mob type: " + type);
        }
        return identifier;
    }

    public static String normalize(String type) {
        String key = type.toLowerCase();
        identifier(key);
        return key;
    }

    public static List<String> all() {
        return SUPPORTED.keySet().stream().sorted().toList();
    }
}
