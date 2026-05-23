package dev.custom.npcs.presentation;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;
import dev.custom.npcs.infrastructure.MobCatalog;
import dev.custom.npcs.infrastructure.NpcSkinCodec;
import dev.custom.npcs.application.DefaultNpcRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public final class NpcCommand extends Command {
    private final PluginBase plugin;
    private final DefaultNpcRegistry registry;
    private final NpcTraitRegistry traitRegistry;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final NpcSelectionContext selectionContext;

    public NpcCommand(PluginBase plugin, DefaultNpcRegistry registry, NpcTraitRegistry traitRegistry, NpcBehaviorRegistry behaviorRegistry, NpcSelectionContext selectionContext) {
        super("npcs", "Manage NPCs", "/npcs", new String[0]);
        this.plugin = plugin;
        this.registry = registry;
        this.traitRegistry = traitRegistry;
        this.behaviorRegistry = behaviorRegistry;
        this.selectionContext = selectionContext;
        setPermission("npcs.command.use");
        configureAutocomplete();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/npcs create <id> [type] [displayName]");
            sender.sendMessage("/npcs remove <id>");
            sender.sendMessage("/npcs spawn <id>");
            sender.sendMessage("/npcs despawn <id>");
            sender.sendMessage("/npcs rename <id> <name>");
            sender.sendMessage("/npcs movehere <id>");
            sender.sendMessage("/npcs teleport <id>");
            sender.sendMessage("/npcs list");
            sender.sendMessage("/npcs info <id>");
            sender.sendMessage("/npcs select <id>");
            sender.sendMessage("/npcs type <id> <npc|human|mob>");
            sender.sendMessage("/npcs mob <id> <mobType>");
            sender.sendMessage("/npcs skin from-player <id> [player]");
            sender.sendMessage("/npcs skin from-file <id> <path>");
            sender.sendMessage("/npcs trait set <id> <trait> <value>");
            sender.sendMessage("/npcs behavior add <id> <behavior>");
            return true;
        }
        try {
            return switch (args[0].toLowerCase()) {
                case "create" -> create(sender, args);
                case "remove" -> remove(sender, args);
                case "spawn" -> spawn(sender, args);
                case "despawn" -> despawn(sender, args);
                case "rename" -> rename(sender, args);
                case "movehere" -> moveHere(sender, args);
                case "teleport", "tp" -> teleport(sender, args);
                case "list" -> list(sender);
                case "info" -> info(sender, args);
                case "select" -> select(sender, args);
                case "type" -> type(sender, args);
                case "mob" -> mob(sender, args);
                case "skin" -> skin(sender, args);
                case "trait" -> trait(sender, args);
                case "behavior" -> behavior(sender, args);
                default -> unknown(sender);
            };
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        } catch (IOException exception) {
            sender.sendMessage("No se pudo cargar el skin: " + exception.getMessage());
            return true;
        }
    }

    private boolean create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs create <id> [type] [displayName]");
            return true;
        }
        String id = args[1];
        int nameIndex = 2;
        NpcEntityType entityType = NpcEntityType.NPC;
        if (args.length >= 3 && isEntityType(args[2])) {
            entityType = NpcEntityType.parse(args[2]);
            nameIndex = 3;
        }
        String displayName = args.length > nameIndex ? join(args, nameIndex) : id;
        registry.create(id, displayName, fromPlayer(player));
        if (entityType != NpcEntityType.NPC) {
            registry.changeType(id, entityType);
        }
        if (entityType == NpcEntityType.MOB) {
            registry.setMetadata(id, "mobType", MobCatalog.defaultMobType());
        }
        registry.spawn(id);
        selectionContext.select(player.getUniqueId(), id);
        sender.sendMessage("NPC creado: " + id + " [" + entityType.id() + "]");
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs remove <id>");
            return true;
        }
        sender.sendMessage(registry.remove(args[1]) ? "NPC eliminado: " + args[1] : "NPC no encontrado: " + args[1]);
        return true;
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs spawn <id>");
            return true;
        }
        registry.spawn(args[1]);
        sender.sendMessage("NPC spawnado: " + args[1]);
        return true;
    }

    private boolean despawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs despawn <id>");
            return true;
        }
        registry.despawn(args[1]);
        sender.sendMessage("NPC ocultado: " + args[1]);
        return true;
    }

    private boolean rename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Uso: /npcs rename <id> <name>");
            return true;
        }
        registry.rename(args[1], join(args, 2));
        sender.sendMessage("NPC renombrado: " + args[1]);
        return true;
    }

    private boolean moveHere(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        String id = resolveId(player, args, 1, "Uso: /npcs movehere <id>");
        if (id == null) {
            return true;
        }
        registry.move(id, fromPlayer(player));
        registry.find(id).filter(NpcHandle::spawned).ifPresent(handle -> registry.spawn(id));
        sender.sendMessage("NPC movido: " + id);
        return true;
    }

    private boolean teleport(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        String id = resolveId(player, args, 1, "Uso: /npcs teleport <id>");
        if (id == null) {
            return true;
        }
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("NPC no encontrado: " + id));
        Location target = new Location(handle.profile().location().x(), handle.profile().location().y(), handle.profile().location().z(), handle.profile().location().yaw(), handle.profile().location().pitch(), player.getServer().getLevelByName(handle.profile().location().world()));
        player.teleport(target);
        sender.sendMessage("Teleportado a: " + id);
        return true;
    }

    private boolean list(CommandSender sender) {
        if (registry.all().isEmpty()) {
            sender.sendMessage("No hay NPCs registrados.");
            return true;
        }
        for (NpcHandle handle : registry.all()) {
            sender.sendMessage(handle.profile().id() + " -> " + handle.profile().displayName() + " [" + handle.profile().location().world() + "]" + (handle.spawned() ? " active" : " stored"));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs info <id>");
            return true;
        }
        NpcHandle handle = registry.find(args[1]).orElseThrow(() -> new IllegalArgumentException("NPC no encontrado: " + args[1]));
        sender.sendMessage("id: " + handle.profile().id());
        sender.sendMessage("name: " + handle.profile().displayName());
        sender.sendMessage("type: " + handle.profile().entityType().id());
        if (handle.profile().entityType() == NpcEntityType.MOB) {
            sender.sendMessage("mob: " + handle.profile().metadata().getOrDefault("mobType", MobCatalog.defaultMobType()));
        }
        sender.sendMessage("world: " + handle.profile().location().world());
        sender.sendMessage("spawned: " + handle.spawned());
        sender.sendMessage("visual: " + handle.profile().visual().skinId());
        sender.sendMessage("traits: " + handle.profile().traits());
        sender.sendMessage("behaviors: " + handle.profile().behaviors());
        return true;
    }

    private boolean select(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs select <id>");
            return true;
        }
        registry.find(args[1]).orElseThrow(() -> new IllegalArgumentException("NPC no encontrado: " + args[1]));
        selectionContext.select(player.getUniqueId(), args[1]);
        sender.sendMessage("NPC seleccionado: " + args[1]);
        return true;
    }

    private boolean trait(CommandSender sender, String[] args) {
        if (args.length < 5 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage("Uso: /npcs trait set <id> <trait> <value>");
            return true;
        }
        registry.setTrait(args[2], args[3], args[4]);
        sender.sendMessage("Trait actualizado para: " + args[2]);
        return true;
    }

    private boolean type(CommandSender sender, String[] args) {
        Player player = sender instanceof Player value ? value : null;
        String id = resolveId(player, args, 1, "Uso: /npcs type <id> <npc|human|mob>");
        if (id == null) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Uso: /npcs type <id> <npc|human|mob>");
            return true;
        }
        NpcEntityType entityType = NpcEntityType.parse(args[2]);
        registry.changeType(id, entityType);
        if (entityType == NpcEntityType.MOB) {
            registry.setMetadata(id, "mobType", registry.profile(id).metadata().getOrDefault("mobType", MobCatalog.defaultMobType()));
        }
        sender.sendMessage("Tipo actualizado para: " + id + " -> " + entityType.id());
        return true;
    }

    private boolean mob(CommandSender sender, String[] args) {
        Player player = sender instanceof Player value ? value : null;
        String id = resolveId(player, args, 1, "Uso: /npcs mob <id> <mobType>");
        if (id == null) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Uso: /npcs mob <id> <mobType>");
            return true;
        }
        String mobType = MobCatalog.normalize(args[2]);
        registry.changeType(id, NpcEntityType.MOB);
        registry.setMetadata(id, "mobType", mobType);
        sender.sendMessage("Mob actualizado para: " + id + " -> " + mobType);
        return true;
    }

    private boolean skin(CommandSender sender, String[] args) throws IOException {
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs skin <from-player|from-file> ...");
            return true;
        }
        return switch (args[1].toLowerCase()) {
            case "from-player" -> skinFromPlayer(sender, args);
            case "from-file" -> skinFromFile(sender, args);
            default -> {
                sender.sendMessage("Uso: /npcs skin <from-player|from-file> ...");
                yield true;
            }
        };
    }

    private boolean skinFromPlayer(CommandSender sender, String[] args) {
        Player commandPlayer = sender instanceof Player value ? value : null;
        String id = resolveId(commandPlayer, args, 2, "Uso: /npcs skin from-player <id> [player]");
        if (id == null) {
            return true;
        }
        Player source;
        if (args.length >= 4) {
            source = sender.getServer().getPlayerExact(args[3]);
            if (source == null) {
                throw new IllegalArgumentException("Jugador no encontrado: " + args[3]);
            }
        } else {
            source = requirePlayer(sender);
        }
        registry.changeType(id, NpcEntityType.HUMAN);
        registry.updateVisual(id, NpcSkinCodec.fromSkin(source.getSkin(), "player:" + source.getName().toLowerCase()));
        registry.setMetadata(id, "skinSource", "player");
        registry.setMetadata(id, "skinOwner", source.getName());
        sender.sendMessage("Skin aplicada desde jugador a: " + id);
        return true;
    }

    private boolean skinFromFile(CommandSender sender, String[] args) throws IOException {
        Player player = sender instanceof Player value ? value : null;
        String id = resolveId(player, args, 2, "Uso: /npcs skin from-file <id> <path>");
        if (id == null) {
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("Uso: /npcs skin from-file <id> <path>");
            return true;
        }
        File file = resolveSkinFile(join(args, 3));
        registry.changeType(id, NpcEntityType.HUMAN);
        registry.updateVisual(id, NpcSkinCodec.fromFile(file));
        registry.setMetadata(id, "skinSource", "file");
        registry.setMetadata(id, "skinFile", file.getPath());
        sender.sendMessage("Skin aplicada desde archivo a: " + id);
        return true;
    }

    private boolean behavior(CommandSender sender, String[] args) {
        if (args.length < 4 || !"add".equalsIgnoreCase(args[1])) {
            sender.sendMessage("Uso: /npcs behavior add <id> <behavior>");
            return true;
        }
        registry.addBehavior(args[2], args[3]);
        sender.sendMessage("Behavior agregado a: " + args[2]);
        return true;
    }

    private boolean unknown(CommandSender sender) {
        sender.sendMessage("Subcomando desconocido.");
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw new IllegalArgumentException("Este comando requiere un jugador.");
    }

    private NpcLocation fromPlayer(Player player) {
        return new NpcLocation(player.getLevel().getName(), player.x, player.y, player.z, (float) player.yaw, (float) player.pitch);
    }

    private String join(String[] args, int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }

    private String resolveId(Player player, String[] args, int index, String usage) {
        if (player == null) {
            if (args.length > index) {
                return args[index];
            }
            throw new IllegalArgumentException(usage);
        }
        if (args.length > index) {
            return args[index];
        }
        Optional<String> selected = selectionContext.selection(player.getUniqueId());
        if (selected.isPresent()) {
            return selected.get();
        }
        player.sendMessage(usage);
        return null;
    }

    private File resolveSkinFile(String rawPath) {
        File file = new File(rawPath);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), rawPath);
        }
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Archivo no encontrado: " + file.getPath());
        }
        return file;
    }

    private void configureAutocomplete() {
        Supplier<java.util.Collection<String>> ids = () -> registry.all().stream().map(handle -> handle.profile().id()).toList();
        Supplier<java.util.Collection<String>> mobs = MobCatalog::all;
        Supplier<java.util.Collection<String>> behaviors = () -> {
            java.util.List<String> values = new java.util.ArrayList<>();
            for (NpcBehavior behavior : behaviorRegistry.all()) {
                values.add(behavior.key());
            }
            return values;
        };
        Supplier<java.util.Collection<String>> traits = () -> {
            java.util.List<String> values = new java.util.ArrayList<>();
            for (dev.custom.npcs.api.NpcTrait trait : traitRegistry.all()) {
                values.add(trait.key());
            }
            return values;
        };
        addCommandParameters("create", new CommandParameter[]{
                CommandParameter.newEnum("create", new String[]{"create"}),
                CommandParameter.newType("id", cn.nukkit.command.data.CommandParamType.STRING),
                CommandParameter.newEnum("entityType", true, new String[]{"npc", "human", "mob"}),
                CommandParameter.newType("displayName", true, cn.nukkit.command.data.CommandParamType.TEXT)
        });
        addCommandParameters("remove", new CommandParameter[]{
                CommandParameter.newEnum("remove", new String[]{"remove"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("spawn", new CommandParameter[]{
                CommandParameter.newEnum("spawn", new String[]{"spawn"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("despawn", new CommandParameter[]{
                CommandParameter.newEnum("despawn", new String[]{"despawn"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("rename", new CommandParameter[]{
                CommandParameter.newEnum("rename", new String[]{"rename"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newType("name", cn.nukkit.command.data.CommandParamType.TEXT)
        });
        addCommandParameters("movehere", new CommandParameter[]{
                CommandParameter.newEnum("movehere", new String[]{"movehere"}),
                CommandParameter.newEnum("id", true, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("teleport", new CommandParameter[]{
                CommandParameter.newEnum("teleport", new String[]{"teleport", "tp"}),
                CommandParameter.newEnum("id", true, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("info", new CommandParameter[]{
                CommandParameter.newEnum("info", new String[]{"info"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("select", new CommandParameter[]{
                CommandParameter.newEnum("select", new String[]{"select"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids))
        });
        addCommandParameters("list", new CommandParameter[]{
                CommandParameter.newEnum("list", new String[]{"list"})
        });
        addCommandParameters("type", new CommandParameter[]{
                CommandParameter.newEnum("type", new String[]{"type"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newEnum("entityType", new String[]{"npc", "human", "mob"})
        });
        addCommandParameters("mob", new CommandParameter[]{
                CommandParameter.newEnum("mob", new String[]{"mob"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newEnum("mobType", false, new CommandEnum("npcMobTypes", mobs))
        });
        addCommandParameters("skinPlayer", new CommandParameter[]{
                CommandParameter.newEnum("skin", new String[]{"skin"}),
                CommandParameter.newEnum("mode", new String[]{"from-player"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newType("player", true, cn.nukkit.command.data.CommandParamType.TARGET)
        });
        addCommandParameters("skinFile", new CommandParameter[]{
                CommandParameter.newEnum("skin", new String[]{"skin"}),
                CommandParameter.newEnum("mode", new String[]{"from-file"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newType("path", cn.nukkit.command.data.CommandParamType.FILE_PATH)
        });
        addCommandParameters("trait", new CommandParameter[]{
                CommandParameter.newEnum("trait", new String[]{"trait"}),
                CommandParameter.newEnum("action", new String[]{"set"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newEnum("traitKey", false, new CommandEnum("npcTraits", traits)),
                CommandParameter.newType("value", cn.nukkit.command.data.CommandParamType.STRING)
        });
        addCommandParameters("behavior", new CommandParameter[]{
                CommandParameter.newEnum("behavior", new String[]{"behavior"}),
                CommandParameter.newEnum("action", new String[]{"add"}),
                CommandParameter.newEnum("id", false, new CommandEnum("npcIds", ids)),
                CommandParameter.newEnum("behaviorKey", false, new CommandEnum("npcBehaviors", behaviors))
        });
        enableParamTree();
    }

    private boolean isEntityType(String value) {
        try {
            NpcEntityType.parse(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
