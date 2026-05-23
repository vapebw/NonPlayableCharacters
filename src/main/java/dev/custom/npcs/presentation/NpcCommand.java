package dev.custom.npcs.presentation;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcLocation;
import dev.custom.npcs.application.DefaultNpcRegistry;

import java.util.Optional;

public final class NpcCommand extends Command {
    private final DefaultNpcRegistry registry;
    private final NpcSelectionContext selectionContext;

    public NpcCommand(DefaultNpcRegistry registry, NpcSelectionContext selectionContext) {
        super("npcs", "Manage NPCs", "/npcs", new String[0]);
        this.registry = registry;
        this.selectionContext = selectionContext;
        setPermission("npcs.command.use");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/npcs create <id> [displayName]");
            sender.sendMessage("/npcs remove <id>");
            sender.sendMessage("/npcs spawn <id>");
            sender.sendMessage("/npcs despawn <id>");
            sender.sendMessage("/npcs rename <id> <name>");
            sender.sendMessage("/npcs movehere <id>");
            sender.sendMessage("/npcs teleport <id>");
            sender.sendMessage("/npcs list");
            sender.sendMessage("/npcs info <id>");
            sender.sendMessage("/npcs select <id>");
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
                case "trait" -> trait(sender, args);
                case "behavior" -> behavior(sender, args);
                default -> unknown(sender);
            };
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
    }

    private boolean create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            sender.sendMessage("Uso: /npcs create <id> [displayName]");
            return true;
        }
        String id = args[1];
        String displayName = args.length >= 3 ? join(args, 2) : id;
        registry.create(id, displayName, fromPlayer(player));
        registry.spawn(id);
        selectionContext.select(player.getUniqueId(), id);
        sender.sendMessage("NPC creado: " + id);
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
        sender.sendMessage("world: " + handle.profile().location().world());
        sender.sendMessage("spawned: " + handle.spawned());
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
}
