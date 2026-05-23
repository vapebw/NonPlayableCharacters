package dev.custom.npcs;

import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.bootstrap.NpcBootstrap;

public final class NonPlayableCharactersPlugin extends PluginBase {
    private NpcBootstrap bootstrap;

    @Override
    public void onLoad() {
        getLogger().info("Loading NonPlayableCharacters...");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bootstrap = new NpcBootstrap(this);
        bootstrap.start();
        getLogger().info("NonPlayableCharacters enabled.");
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.close();
        }
        getLogger().info("NonPlayableCharacters disabled.");
    }
}
