package dev.custom.npcs.infrastructure;

import cn.nukkit.Player;
import cn.nukkit.entity.passive.EntityNpc;
import cn.nukkit.level.Location;
import dev.custom.npcs.api.NpcProfile;

public final class NpcEntityBridge {
    private final EntityNpc entity;

    public NpcEntityBridge(EntityNpc entity) {
        this.entity = entity;
    }

    public long runtimeId() {
        return entity.getId();
    }

    public EntityNpc entity() {
        return entity;
    }

    public Location location() {
        return entity.getLocation();
    }

    public void apply(NpcProfile profile) {
        entity.setNameTag(profile.displayName());
        entity.setNameTagVisible(profile.flags().nameVisible());
        entity.setNameTagAlwaysVisible(profile.flags().nameVisible());
        entity.setImmobile(profile.flags().immobile());
        entity.setScale(1.0f);
        if (!profile.visual().skinData().isEmpty()) {
            entity.getDialog().setSkinData(profile.visual().skinData());
        }
        entity.setDataProperty(cn.nukkit.entity.Entity.INTERACT_TEXT, profile.metadata().getOrDefault("interact_text", profile.displayName()));
        entity.setRotation(profile.location().yaw(), profile.location().pitch());
    }

    public Player nearestPlayer(double maxDistance) {
        Player best = null;
        double bestDistance = maxDistance * maxDistance;
        for (Player player : entity.getLevel().getPlayers().values()) {
            double distance = player.distanceSquared(entity);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    public void rotate(double yaw, double pitch) {
        entity.setRotation(yaw, pitch);
    }

    public void teleport(Location location) {
        entity.teleport(location);
    }

    public void spawn() {
        entity.spawnToAll();
    }

    public void despawn() {
        entity.close();
    }
}
