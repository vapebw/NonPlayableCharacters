package dev.custom.npcs.api;

import cn.nukkit.Player;

public interface NpcInteraction {
    String key();

    void execute(NpcHandle handle, Player player);
}
