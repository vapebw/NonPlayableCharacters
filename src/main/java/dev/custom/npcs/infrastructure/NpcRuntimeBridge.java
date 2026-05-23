package dev.custom.npcs.infrastructure;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Location;
import dev.custom.npcs.api.NpcProfile;

public interface NpcRuntimeBridge {
    long runtimeId();

    Entity entity();

    Location location();

    void apply(NpcProfile profile);

    Player nearestPlayer(double maxDistance);

    void rotate(double yaw, double pitch);

    void teleport(Location location);

    void spawn();

    void despawn();
}
