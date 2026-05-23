package dev.custom.npcs.api;

public record NpcVisualProfile(String skinId, String geometryName, String geometryData, String skinData) {
    public static NpcVisualProfile empty() {
        return new NpcVisualProfile("", "", "", "");
    }
}
