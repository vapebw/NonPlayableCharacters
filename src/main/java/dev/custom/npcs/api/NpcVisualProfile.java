package dev.custom.npcs.api;

public record NpcVisualProfile(String skinId, String geometryName, String geometryData, String skinData, String skinResourcePatch) {
    public static NpcVisualProfile empty() {
        return new NpcVisualProfile("", "", "", "", "");
    }

    public NpcVisualProfile withSkinId(String value) {
        return new NpcVisualProfile(value, geometryName, geometryData, skinData, skinResourcePatch);
    }

    public NpcVisualProfile withGeometryName(String value) {
        return new NpcVisualProfile(skinId, value, geometryData, skinData, skinResourcePatch);
    }

    public NpcVisualProfile withGeometryData(String value) {
        return new NpcVisualProfile(skinId, geometryName, value, skinData, skinResourcePatch);
    }

    public NpcVisualProfile withSkinData(String value) {
        return new NpcVisualProfile(skinId, geometryName, geometryData, value, skinResourcePatch);
    }

    public NpcVisualProfile withSkinResourcePatch(String value) {
        return new NpcVisualProfile(skinId, geometryName, geometryData, skinData, value);
    }
}
