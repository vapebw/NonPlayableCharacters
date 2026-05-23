# NonPlayableCharacters

NonPlayableCharacters is a high-level NPC plugin and developer API for PowerNukkitX.

It is designed to work both as a standalone plugin and as a reusable foundation for other plugins that need persistent, scriptable, command-managed NPCs.

## Highlights

- Clean public API for other plugins
- Persistent NPC storage in YAML
- Built-in `/npcs` command tree
- Runtime built on top of PowerNukkitX `EntityNpc`
- Extensible traits and behaviors
- Player-based NPC selection flow for fast editing
- Layered structure focused on long-term maintainability

## Features

- Create, spawn, despawn, move, rename and remove NPCs
- Store NPC identity, location, traits, behaviors and metadata
- Register custom interactions from external plugins
- Apply built-in traits such as visibility, look-at-player and immobility
- Attach built-in behaviors such as idle, click action and look-at-player

## Commands

```text
/npcs create <id> [displayName]
/npcs remove <id>
/npcs spawn <id>
/npcs despawn <id>
/npcs rename <id> <name>
/npcs movehere <id>
/npcs teleport <id>
/npcs list
/npcs info <id>
/npcs select <id>
/npcs trait set <id> <trait> <value>
/npcs behavior add <id> <behavior>
```

## Public API

The plugin registers `NpcApi` through the PowerNukkitX `ServiceManager`.

### Get the API

```java
import cn.nukkit.Server;
import cn.nukkit.plugin.service.RegisteredServiceProvider;
import dev.custom.npcs.api.NpcApi;

RegisteredServiceProvider<NpcApi> provider = Server.getInstance()
        .getServiceManager()
        .getProvider(NpcApi.class);

if (provider == null) {
    throw new IllegalStateException("NonPlayableCharacters is not available");
}

NpcApi api = provider.getProvider();
```

### Create and spawn an NPC

```java
import dev.custom.npcs.api.NpcLocation;

api.registry().create(
        "guide",
        "Spawn Guide",
        new NpcLocation("spawn", 0.5, 64.0, 0.5, 180.0f, 0.0f)
);

api.registry().setTrait("guide", "visible_name", "true");
api.registry().setTrait("guide", "look_at_player", "true");
api.registry().addBehavior("guide", "idle");
api.registry().addBehavior("guide", "click_action");
api.registry().spawn("guide");
```

### Move or rename an existing NPC

```java
api.registry().rename("guide", "Main Guide");
api.registry().move("guide", new NpcLocation("spawn", 10.0, 65.0, 4.0, 90.0f, 0.0f));
```

### Access registered NPCs

```java
api.registry().find("guide").ifPresent(handle -> {
    String id = handle.profile().id();
    String name = handle.profile().displayName();
    boolean spawned = handle.spawned();
});
```

### Register a custom interaction

```java
import cn.nukkit.Player;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcInteraction;

api.interactions().register(new NpcInteraction() {
    @Override
    public String key() {
        return "open_shop";
    }

    @Override
    public void execute(NpcHandle handle, Player player) {
        player.sendMessage("Opening shop for " + handle.profile().displayName());
    }
});
```

To connect that interaction to an NPC in the current implementation, store an interaction key in the NPC metadata flow used by your plugin logic.

## Built-in Traits

- `visible_name`
- `look_at_player`
- `immobile`
- `protected`
- `push_protected`

## Built-in Behaviors

- `idle`
- `look_at_player`
- `click_action`

## Project Layout

- `api`: public contracts intended for plugin developers
- `application`: orchestration, registry logic and runtime coordination
- `domain`: trait and behavior registries
- `infrastructure`: YAML persistence and PowerNukkitX bridge layer
- `presentation`: commands, listeners and selection flow

## Developer Notes

- The main service entry point is `NpcApi`
- The main runtime facade is `NpcRegistry`
- Public types are intentionally small and stable
- Internal runtime details are hidden behind `NpcHandle` and the infrastructure layer
- The plugin is organized so future SQL persistence or more advanced behaviors can be added without rewriting the public API

## Build

This project targets Java 21 and Gradle.

```text
gradle test
gradle build
```
