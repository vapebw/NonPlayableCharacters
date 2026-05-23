package dev.custom.npcs.infrastructure;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Location;
import dev.custom.npcs.api.NpcProfile;

public final class HumanEntityBridge implements NpcRuntimeBridge {
    private final EntityHuman entity;

    public HumanEntityBridge(EntityHuman entity) {
        this.entity = entity;
    }

    @Override
    public long runtimeId() {
        return entity.getId();
    }

    @Override
    public Entity entity() {
        return entity;
    }

    public EntityHuman human() {
        return entity;
    }

    @Override
    public Location location() {
        return entity.getLocation();
    }

    @Override
    public void apply(NpcProfile profile) {
        entity.setNameTag(profile.displayName());
        entity.setNameTagVisible(profile.flags().nameVisible());
        entity.setNameTagAlwaysVisible(profile.flags().nameVisible());
        entity.setImmobile(profile.flags().immobile());
        entity.setScale(1.0f);
        entity.setRotation(profile.location().yaw(), profile.location().pitch());
        if (!profile.visual().skinData().isEmpty()) {
            Skin skin = NpcSkinCodec.decode(profile.visual());
            entity.setSkin(skin);
        }
    }

    @Override
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

    @Override
    public void rotate(double yaw, double pitch) {
        entity.setRotation(yaw, pitch);
    }

    @Override
    public void teleport(Location location) {
        entity.teleport(location);
    }

    @Override
    public void spawn() {
        entity.spawnToAll();
    }

    @Override
    public void despawn() {
        entity.close();
    }
}
