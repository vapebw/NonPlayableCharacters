package dev.custom.npcs.presentation;

import cn.nukkit.Player;
import cn.nukkit.form.element.simple.ButtonImage;
import cn.nukkit.form.response.CustomResponse;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.SimpleForm;
import cn.nukkit.plugin.PluginBase;
import dev.custom.npcs.api.NpcBehavior;
import dev.custom.npcs.api.NpcEntityType;
import dev.custom.npcs.api.NpcHandle;
import dev.custom.npcs.api.NpcTrait;
import dev.custom.npcs.application.DefaultNpcRegistry;
import dev.custom.npcs.domain.NpcBehaviorRegistry;
import dev.custom.npcs.domain.NpcTraitRegistry;
import dev.custom.npcs.infrastructure.MobCatalog;
import dev.custom.npcs.infrastructure.NpcSkinCodec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NpcManageForms {
    private final PluginBase plugin;
    private final DefaultNpcRegistry registry;
    private final NpcTraitRegistry traitRegistry;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final NpcSelectionContext selectionContext;
    private final ConcurrentMap<UUID, NpcManageSession> sessions = new ConcurrentHashMap<>();

    public NpcManageForms(PluginBase plugin, DefaultNpcRegistry registry, NpcTraitRegistry traitRegistry, NpcBehaviorRegistry behaviorRegistry, NpcSelectionContext selectionContext) {
        this.plugin = plugin;
        this.registry = registry;
        this.traitRegistry = traitRegistry;
        this.behaviorRegistry = behaviorRegistry;
        this.selectionContext = selectionContext;
    }

    public void openMain(Player player) {
        NpcManageSession session = session(player);
        session.selectedId(selectionContext.selection(player.getUniqueId()).orElse(session.selectedId()));
        String selected = session.selectedId().isEmpty() ? "None" : session.selectedId();
        new SimpleForm("NPC Manager", "Selected: " + selected)
                .addButton("Create NPC", image("textures/ui/color_plus"))
                .addButton("Browse NPCs", image("textures/ui/book_edit_default"))
                .addButton("Quick Manage Selected", image("textures/ui/icon_setting"))
                .addButton("Close", image("textures/ui/cancel"))
                .onSubmit((viewer, response) -> {
                    if (response == null) {
                        return;
                    }
                    switch (response.buttonId()) {
                        case 0 -> openCreate(viewer);
                        case 1 -> openList(viewer);
                        case 2 -> openSelected(viewer);
                        default -> {
                        }
                    }
                })
                .send(player);
    }

    public void openSelected(Player player) {
        Optional<String> selected = selectionContext.selection(player.getUniqueId());
        if (selected.isEmpty()) {
            player.sendMessage("No NPC selected.");
            openList(player);
            return;
        }
        openDetails(player, selected.get());
    }

    private void openList(Player player) {
        SimpleForm form = new SimpleForm("NPC List", "Select an NPC to manage");
        int count = 0;
        for (NpcHandle handle : registry.all()) {
            String line = handle.profile().id() + " [" + handle.profile().entityType().id() + "]" + (handle.spawned() ? " active" : " stored");
            form.addButton(line, image("textures/ui/multiplayer_glyph_color"), viewer -> {
                selectionContext.select(viewer.getUniqueId(), handle.profile().id());
                session(viewer).selectedId(handle.profile().id());
                openDetails(viewer, handle.profile().id());
            });
            count++;
        }
        if (count == 0) {
            form.addButton("No NPCs registered");
        }
        form.addButton("Back", image("textures/ui/refresh_light"), this::openMain)
                .send(player);
    }

    private void openCreate(Player player) {
        NpcManageSession session = session(player);
        List<String> types = List.of("npc", "human", "mob");
        List<String> mobs = MobCatalog.all();
        int typeIndex = types.indexOf(session.entityType().id());
        if (typeIndex < 0) {
            typeIndex = 0;
        }
        int mobIndex = Math.max(0, mobs.indexOf(session.mobType().isEmpty() ? MobCatalog.defaultMobType() : session.mobType()));
        new CustomForm("Create NPC")
                .addInput("Internal id", "guide", "")
                .addInput("Display name", "Spawn Guide", "")
                .addDropdown("Entity type", types, typeIndex)
                .addDropdown("Mob type", mobs, mobIndex)
                .onSubmit((viewer, response) -> submitCreate(viewer, response))
                .onClose(this::openMain)
                .send(player);
    }

    private void submitCreate(Player player, CustomResponse response) {
        if (response == null) {
            return;
        }
        String id = safe(response.getInputResponse(0));
        if (id.isEmpty()) {
            player.sendMessage("The id cannot be empty.");
            openCreate(player);
            return;
        }
        String displayName = safe(response.getInputResponse(1));
        List<String> types = List.of("npc", "human", "mob");
        NpcEntityType entityType = NpcEntityType.parse(types.get(response.getDropdownResponse(2).elementId()));
        List<String> mobs = MobCatalog.all();
        String mobType = mobs.get(response.getDropdownResponse(3).elementId());
        registry.create(id, displayName.isEmpty() ? id : displayName, new dev.custom.npcs.api.NpcLocation(player.getLevel().getName(), player.x, player.y, player.z, (float) player.yaw, (float) player.pitch));
        registry.changeType(id, entityType);
        if (entityType == NpcEntityType.MOB) {
            registry.setMetadata(id, "mobType", mobType);
        }
        registry.spawn(id);
        selectionContext.select(player.getUniqueId(), id);
        NpcManageSession session = session(player);
        session.selectedId(id);
        session.entityType(entityType);
        if (entityType == NpcEntityType.MOB) {
            session.mobType(mobType);
        }
        openDetails(player, id);
    }

    private void openDetails(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        SimpleForm form = new SimpleForm(handle.profile().displayName(), describe(handle))
                .addButton(handle.spawned() ? "Despawn" : "Spawn", image("textures/ui/realms_green_check"))
                .addButton("Rename", image("textures/ui/editIcon"))
                .addButton("Move Here", image("textures/ui/icon_recipe_nature"))
                .addButton("Teleport To NPC", image("textures/ui/FriendsDiversity"))
                .addButton("Entity Type", image("textures/ui/icon_import"))
                .addButton("Flags", image("textures/ui/toggle_on"))
                .addButton("Behaviors", image("textures/ui/feedIcon"))
                .addButton("Traits", image("textures/ui/icon_book_writable"))
                .addButton("Skin", image("textures/ui/dressing_room_skins"))
                .addButton("Delete NPC", image("textures/ui/trash_default"))
                .addButton("Back", image("textures/ui/refresh_light"))
                .onSubmit((viewer, response) -> {
                    if (response == null) {
                        return;
                    }
                    switch (response.buttonId()) {
                        case 0 -> toggleSpawn(viewer, id);
                        case 1 -> openRename(viewer, id);
                        case 2 -> moveHere(viewer, id);
                        case 3 -> teleport(viewer, id);
                        case 4 -> openType(viewer, id);
                        case 5 -> openFlags(viewer, id);
                        case 6 -> openBehaviors(viewer, id);
                        case 7 -> openTraits(viewer, id);
                        case 8 -> openSkin(viewer, id);
                        case 9 -> openDelete(viewer, id);
                        default -> openList(viewer);
                    }
                });
        form.send(player);
    }

    private void toggleSpawn(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        if (handle.spawned()) {
            registry.despawn(id);
        } else {
            registry.spawn(id);
        }
        openDetails(player, id);
    }

    private void moveHere(Player player, String id) {
        registry.move(id, new dev.custom.npcs.api.NpcLocation(player.getLevel().getName(), player.x, player.y, player.z, (float) player.yaw, (float) player.pitch));
        registry.spawn(id);
        openDetails(player, id);
    }

    private void teleport(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        player.teleport(new cn.nukkit.level.Location(handle.profile().location().x(), handle.profile().location().y(), handle.profile().location().z(), handle.profile().location().yaw(), handle.profile().location().pitch(), player.getServer().getLevelByName(handle.profile().location().world())));
        openDetails(player, id);
    }

    private void openRename(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        new CustomForm("Rename NPC")
                .addInput("Display name", "Spawn Guide", handle.profile().displayName())
                .onSubmit((viewer, response) -> {
                    if (response == null) {
                        return;
                    }
                    String name = safe(response.getInputResponse(0));
                    if (!name.isEmpty()) {
                        registry.rename(id, name);
                    }
                    openDetails(viewer, id);
                })
                .onClose(viewer -> openDetails(viewer, id))
                .send(player);
    }

    private void openType(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        List<String> types = List.of("npc", "human", "mob");
        List<String> mobs = MobCatalog.all();
        int typeIndex = Math.max(0, types.indexOf(handle.profile().entityType().id()));
        int mobIndex = Math.max(0, mobs.indexOf(handle.profile().metadata().getOrDefault("mobType", MobCatalog.defaultMobType())));
        new CustomForm("Entity Type")
                .addDropdown("Entity type", types, typeIndex)
                .addDropdown("Mob type", mobs, mobIndex)
                .onSubmit((viewer, response) -> {
                    if (response == null) {
                        return;
                    }
                    NpcEntityType entityType = NpcEntityType.parse(types.get(response.getDropdownResponse(0).elementId()));
                    registry.changeType(id, entityType);
                    if (entityType == NpcEntityType.MOB) {
                        registry.setMetadata(id, "mobType", mobs.get(response.getDropdownResponse(1).elementId()));
                    }
                    openDetails(viewer, id);
                })
                .onClose(viewer -> openDetails(viewer, id))
                .send(player);
    }

    private void openFlags(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        new CustomForm("Flags")
                .addToggle("Visible name", handle.profile().flags().nameVisible())
                .addToggle("Look at player", handle.profile().flags().lookAtPlayer())
                .addToggle("Immobile", handle.profile().flags().immobile())
                .addToggle("Protected from damage", handle.profile().flags().protectedFromDamage())
                .addToggle("Push protected", handle.profile().flags().pushProtected())
                .onSubmit((viewer, response) -> {
                    if (response == null) {
                        return;
                    }
                    registry.setTrait(id, "visible_name", String.valueOf(response.getToggleResponse(0)));
                    registry.setTrait(id, "look_at_player", String.valueOf(response.getToggleResponse(1)));
                    registry.setTrait(id, "immobile", String.valueOf(response.getToggleResponse(2)));
                    registry.setTrait(id, "protected", String.valueOf(response.getToggleResponse(3)));
                    registry.setTrait(id, "push_protected", String.valueOf(response.getToggleResponse(4)));
                    openDetails(viewer, id);
                })
                .onClose(viewer -> openDetails(viewer, id))
                .send(player);
    }

    private void openBehaviors(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        List<String> keys = behaviorKeys();
        CustomForm form = new CustomForm("Behaviors");
        for (String key : keys) {
            form.addToggle(key, handle.profile().behaviors().contains(key));
        }
        form.onSubmit((viewer, response) -> {
            if (response == null) {
                return;
            }
            for (int index = 0; index < keys.size(); index++) {
                boolean enabled = response.getToggleResponse(index);
                if (enabled && !handle.profile().behaviors().contains(keys.get(index))) {
                    registry.addBehavior(id, keys.get(index));
                }
            }
            openDetails(viewer, id);
        }).onClose(viewer -> openDetails(viewer, id)).send(player);
    }

    private void openTraits(Player player, String id) {
        NpcHandle handle = registry.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown NPC: " + id));
        List<String> keys = traitKeys();
        CustomForm form = new CustomForm("Trait Values");
        for (String key : keys) {
            form.addInput(key, "true / false / value", handle.profile().traits().getOrDefault(key, ""));
        }
        form.onSubmit((viewer, response) -> {
            if (response == null) {
                return;
            }
            for (int index = 0; index < keys.size(); index++) {
                String value = safe(response.getInputResponse(index));
                if (!value.isEmpty()) {
                    try {
                        registry.setTrait(id, keys.get(index), value);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            openDetails(viewer, id);
        }).onClose(viewer -> openDetails(viewer, id)).send(player);
    }

    private void openSkin(Player player, String id) {
        new CustomForm("Skin")
                .addInput("Apply from player", "Leave empty to use yourself", "")
                .addInput("Apply from file", "Absolute path or plugin data relative path", "")
                .onSubmit((viewer, response) -> submitSkin(viewer, id, response))
                .onClose(viewer -> openDetails(viewer, id))
                .send(player);
    }

    private void submitSkin(Player player, String id, CustomResponse response) {
        if (response == null) {
            return;
        }
        String fromPlayer = safe(response.getInputResponse(0));
        String fromFile = safe(response.getInputResponse(1));
        try {
            if (!fromPlayer.isEmpty()) {
                Player source = player.getServer().getPlayerExact(fromPlayer);
                if (source == null) {
                    player.sendMessage("Player not found: " + fromPlayer);
                    openSkin(player, id);
                    return;
                }
                registry.changeType(id, NpcEntityType.HUMAN);
                registry.updateVisual(id, NpcSkinCodec.fromSkin(source.getSkin(), "player:" + source.getName().toLowerCase()));
                registry.setMetadata(id, "skinSource", "player");
                registry.setMetadata(id, "skinOwner", source.getName());
            } else if (!fromFile.isEmpty()) {
                File file = resolveSkinFile(fromFile);
                registry.changeType(id, NpcEntityType.HUMAN);
                registry.updateVisual(id, NpcSkinCodec.fromFile(file));
                registry.setMetadata(id, "skinSource", "file");
                registry.setMetadata(id, "skinFile", file.getPath());
            } else {
                registry.changeType(id, NpcEntityType.HUMAN);
                registry.updateVisual(id, NpcSkinCodec.fromSkin(player.getSkin(), "player:" + player.getName().toLowerCase()));
                registry.setMetadata(id, "skinSource", "player");
                registry.setMetadata(id, "skinOwner", player.getName());
            }
        } catch (IOException exception) {
            player.sendMessage("Could not load skin: " + exception.getMessage());
        }
        openDetails(player, id);
    }

    private void openDelete(Player player, String id) {
        new SimpleForm("Delete NPC", "Are you sure you want to remove " + id + "?")
                .addButton("Delete", image("textures/ui/trash_default"), viewer -> {
                    registry.remove(id);
                    openList(viewer);
                })
                .addButton("Cancel", image("textures/ui/cancel"), viewer -> openDetails(viewer, id))
                .send(player);
    }

    private ButtonImage image(String path) {
        return new ButtonImage(ButtonImage.Type.PATH, path);
    }

    private String describe(NpcHandle handle) {
        StringBuilder builder = new StringBuilder();
        builder.append("Id: ").append(handle.profile().id()).append('\n');
        builder.append("Type: ").append(handle.profile().entityType().id()).append('\n');
        if (handle.profile().entityType() == NpcEntityType.MOB) {
            builder.append("Mob: ").append(handle.profile().metadata().getOrDefault("mobType", MobCatalog.defaultMobType())).append('\n');
        }
        builder.append("World: ").append(handle.profile().location().world()).append('\n');
        builder.append("Spawned: ").append(handle.spawned()).append('\n');
        builder.append("Traits: ").append(handle.profile().traits()).append('\n');
        builder.append("Behaviors: ").append(handle.profile().behaviors());
        return builder.toString();
    }

    private List<String> behaviorKeys() {
        List<String> keys = new ArrayList<>();
        for (NpcBehavior behavior : behaviorRegistry.all()) {
            keys.add(behavior.key());
        }
        return keys;
    }

    private List<String> traitKeys() {
        List<String> keys = new ArrayList<>();
        for (NpcTrait trait : traitRegistry.all()) {
            keys.add(trait.key());
        }
        return keys;
    }

    private File resolveSkinFile(String rawPath) {
        File file = new File(rawPath);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), rawPath);
        }
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + file.getPath());
        }
        return file;
    }

    private NpcManageSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), ignored -> new NpcManageSession());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
