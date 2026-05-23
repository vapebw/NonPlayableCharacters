package dev.custom.npcs.presentation;

import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.infrastructure.MobCatalog;

public final class NpcManageSession {
    private String selectedId = "";
    private NpcEntityType entityType = NpcEntityType.NPC;
    private String mobType = "";

    public String selectedId() {
        return selectedId;
    }

    public void selectedId(String value) {
        selectedId = value == null ? "" : value;
    }

    public NpcEntityType entityType() {
        return entityType;
    }

    public void entityType(NpcEntityType value) {
        entityType = value == null ? NpcEntityType.NPC : value;
        if (entityType == NpcEntityType.MOB) {
            if (mobType.isEmpty()) {
                mobType = MobCatalog.defaultMobType();
            }
        } else {
            mobType = "";
        }
    }

    public String mobType() {
        return mobType;
    }

    public void mobType(String value) {
        mobType = value == null ? "" : value;
    }
}
