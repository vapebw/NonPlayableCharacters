package dev.custom.npcs.presentation;

import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.infrastructure.MobCatalog;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NpcManageSessionTest {
    @Test
    public void defaultsMobTypeWhenEntityTypeIsMob() {
        NpcManageSession session = new NpcManageSession();

        session.entityType(NpcEntityType.MOB);

        assertEquals(MobCatalog.defaultMobType(), session.mobType());
    }

    @Test
    public void clearsMobTypeWhenEntityTypeChangesAwayFromMob() {
        NpcManageSession session = new NpcManageSession();
        session.entityType(NpcEntityType.MOB);
        session.mobType("skeleton");

        session.entityType(NpcEntityType.HUMAN);

        assertEquals("", session.mobType());
    }
}
