package dev.custom.npcs.api;

public record NpcFlags(boolean immobile, boolean lookAtPlayer, boolean nameVisible, boolean protectedFromDamage, boolean pushProtected) {
    public static NpcFlags defaults() {
        return new NpcFlags(true, false, true, true, true);
    }

    public NpcFlags withImmobile(boolean value) {
        return new NpcFlags(value, lookAtPlayer, nameVisible, protectedFromDamage, pushProtected);
    }

    public NpcFlags withLookAtPlayer(boolean value) {
        return new NpcFlags(immobile, value, nameVisible, protectedFromDamage, pushProtected);
    }

    public NpcFlags withNameVisible(boolean value) {
        return new NpcFlags(immobile, lookAtPlayer, value, protectedFromDamage, pushProtected);
    }

    public NpcFlags withProtectedFromDamage(boolean value) {
        return new NpcFlags(immobile, lookAtPlayer, nameVisible, value, pushProtected);
    }

    public NpcFlags withPushProtected(boolean value) {
        return new NpcFlags(immobile, lookAtPlayer, nameVisible, protectedFromDamage, value);
    }
}
