package dev.custom.npcs.bootstrap;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.ServicePriority;
import cn.nukkit.scheduler.TaskHandler;
import dev.custom.npcs.api.NpcApi;
import dev.custom.npcs.api.NpcFactory;
import dev.custom.npcs.api.NpcRepository;
import dev.custom.npcs.application.DefaultNpcApi;
import dev.custom.npcs.application.DefaultNpcFactory;
import dev.custom.npcs.application.DefaultNpcRegistry;
import dev.custom.npcs.application.NoopNpcRuntime;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcInteractionRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;
import dev.custom.npcs.infrastructure.PowerNukkitNpcRuntime;
import dev.custom.npcs.infrastructure.YamlNpcRepository;
import dev.custom.npcs.presentation.NpcCommand;
import dev.custom.npcs.presentation.NpcInteractionListener;
import dev.custom.npcs.presentation.NpcSelectionContext;

import java.io.File;

public final class NpcBootstrap implements AutoCloseable {
    private final PluginBase plugin;
    private DefaultNpcRegistry registry;
    private TaskHandler taskHandler;

    public NpcBootstrap(PluginBase plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getDataFolder().mkdirs();
        NpcRepository repository = new YamlNpcRepository(new File(plugin.getDataFolder(), "npcs.yml"));
        NpcFactory factory = new DefaultNpcFactory();
        NpcTraitRegistry traitRegistry = new NpcTraitRegistry();
        NpcBehaviorRegistry behaviorRegistry = new NpcBehaviorRegistry();
        NpcInteractionRegistry interactionRegistry = new NpcInteractionRegistry();
        registry = new DefaultNpcRegistry(repository, factory, traitRegistry, behaviorRegistry, interactionRegistry, new NoopNpcRuntime());
        registry.loadAll();
        PowerNukkitNpcRuntime runtime = new PowerNukkitNpcRuntime(plugin, registry, behaviorRegistry, interactionRegistry);
        registry.attachRuntime(runtime);
        NpcApi api = new DefaultNpcApi(registry, factory, traitRegistry, behaviorRegistry, interactionRegistry);
        plugin.getServer().getServiceManager().register(NpcApi.class, api, plugin, ServicePriority.NORMAL);
        NpcSelectionContext selectionContext = new NpcSelectionContext();
        Server server = plugin.getServer();
        server.getPluginManager().registerEvents(new NpcInteractionListener(registry, selectionContext), plugin);
        server.getCommandMap().register("npcs", new NpcCommand(registry, selectionContext));
        registry.spawnPersistent();
        taskHandler = server.getScheduler().scheduleRepeatingTask(plugin, registry::tick, 10);
    }

    @Override
    public void close() {
        if (taskHandler != null) {
            taskHandler.cancel();
        }
        if (registry != null) {
            registry.saveAll();
            registry.despawnAll();
        }
    }
}
