package dev.custom.npcs.api;

public enum NpcEntityType {
    NPC("npc"),
    HUMAN("human"),
    MOB("mob");

    private final String id;

    NpcEntityType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static NpcEntityType parse(String value) {
        for (NpcEntityType type : values()) {
            if (type.id.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NPC entity type: " + value);
    }
}
