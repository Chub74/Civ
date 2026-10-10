package vg.civcraft.mc.citadel.model;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.papermc.paper.adventure.AdventureComponent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.events.ReinforcementCreationEvent;
import vg.civcraft.mc.citadel.listener.ModeListener;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.group.Group;

/**
 * Labels the exposed faces of reinforced blocks around a player with group and health. Each label is a text
 * display that only exists on that player's client, so the server pays in packets, not entities.
 */
public class ReinforcementOverlayManager implements Listener {

    private static final long RUN_INTERVAL_TICKS = 4L;
    /** Runs between full scans for a player standing still, which pick up blocks changed by anything else. */
    private static final int RESCAN_RUNS = 10;
    /** Closest faces win past this, so a wall of reinforced blocks cannot flood a client with entities. */
    private static final int MAX_LABELS = 512;
    /** Lifts a label off its face so it does not z-fight with the block texture. */
    private static final double FACE_OFFSET = 0.01;
    /** Share of a face a label may cover. */
    private static final double FILL = 0.85;
    private static final float MAX_SCALE = 0.6F;
    /** Text displays draw a font pixel as 1/40 of a block and give each line 10 of them. */
    private static final double PIXEL = 0.025;
    private static final int LINE_PIXELS = 10;
    private static final int SPRITE_PIXELS = 9;
    /** Wide enough that no group name wraps. */
    private static final int LINE_WIDTH = 1000;
    private static final int FULL_BRIGHT = 15 << 4 | 15 << 20;
    private static final BlockFace[] SIDES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
    private static final DecimalFormat HEALTH = new DecimalFormat("0");
    private static final WrappedDataWatcher.Serializer VECTOR_SERIALIZER =
        new WrappedDataWatcher.Serializer(Vector3fc.class, EntityDataSerializers.VECTOR3, false);

    private final CitadelSettingManager settings;
    private final Map<UUID, PlayerOverlay> overlays = new HashMap<>();

    public ReinforcementOverlayManager(CitadelSettingManager settings) {
        this.settings = settings;
        settings.getOverlay().registerListener((uuid, setting, oldValue, newValue) -> {
            if (!newValue) {
                clear(Bukkit.getPlayer(uuid));
            }
        });
        new BukkitRunnable() {

            @Override
            public void run() {
                tick();
            }

        }.runTaskTimer(Citadel.getInstance(), RUN_INTERVAL_TICKS, RUN_INTERVAL_TICKS);
    }

    /** Removes every label from every client, for shutdown and reload. */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(overlays.keySet())) {
            clear(Bukkit.getPlayer(uuid));
        }
        overlays.clear();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (settings.getOverlay().getValue(player.getUniqueId())) {
                overlays.computeIfAbsent(player.getUniqueId(), k -> new PlayerOverlay()).tick(player);
            }
        }
    }

    private void clear(Player player) {
        if (player == null) {
            return;
        }
        PlayerOverlay overlay = overlays.remove(player.getUniqueId());
        if (overlay != null) {
            overlay.destroyAll(player);
        }
    }

    private void markDirtyNear(Location location) {
        for (Map.Entry<UUID, PlayerOverlay> entry : overlays.entrySet()) {
            entry.getValue().markDirtyIfNear(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        markDirtyNear(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        markDirtyNear(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReinforce(ReinforcementCreationEvent event) {
        markDirtyNear(event.getReinforcement().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        overlays.remove(event.getPlayer().getUniqueId());
    }

    // The client may already have dropped the labels with its old level, destroying unknown ids is harmless
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        clear(event.getPlayer());
    }

    private record Face(int x, int y, int z, BlockFace side) {
    }

    /** Where a label sits and how much room its face offers. */
    private record Placement(double x, double y, double z, float yaw, float pitch, double width, double height) {
    }

    private record LabelText(Component component, int widthPixels, int lines) {
    }

    private record Content(Component component, float scale, float shift) {
    }

    private record Candidate(Face face, int distanceSquared, Block block, LabelText text) {
    }

    private static final class Label {

        private final int entityId;
        private final Placement placement;
        private Content content;

        private Label(int entityId, Placement placement, Content content) {
            this.entityId = entityId;
            this.placement = placement;
            this.content = content;
        }
    }

    private final class PlayerOverlay {

        private final Map<Face, Label> labels = new HashMap<>();
        private UUID worldId;
        private int eyeX;
        private int eyeY;
        private int eyeZ;
        private int radius;
        private BlockFace facing;
        private int runsSinceScan;
        private boolean dirty = true;

        void tick(Player player) {
            Location eye = player.getEyeLocation();
            int newRadius = settings.getOverlayRadius().getValue(player.getUniqueId());
            BlockFace newFacing = player.getFacing();
            boolean moved = !eye.getWorld().getUID().equals(worldId) || eye.getBlockX() != eyeX
                || eye.getBlockY() != eyeY || eye.getBlockZ() != eyeZ || newRadius != radius || newFacing != facing;
            if (!dirty && !moved && ++runsSinceScan < RESCAN_RUNS) {
                refresh(player);
                return;
            }
            worldId = eye.getWorld().getUID();
            eyeX = eye.getBlockX();
            eyeY = eye.getBlockY();
            eyeZ = eye.getBlockZ();
            radius = newRadius;
            facing = newFacing;
            runsSinceScan = 0;
            dirty = false;
            scan(player, eye.getWorld());
        }

        void markDirtyIfNear(Location location) {
            if (!location.getWorld().getUID().equals(worldId)) {
                return;
            }
            int dx = location.getBlockX() - eyeX;
            int dy = location.getBlockY() - eyeY;
            int dz = location.getBlockZ() - eyeZ;
            // one more block, a change just outside the radius can expose a face just inside it
            if (dx * dx + dy * dy + dz * dz <= (radius + 1) * (radius + 1)) {
                dirty = true;
            }
        }

        private void scan(Player player, World world) {
            ReinforcementManager reinforcements = Citadel.getInstance().getReinforcementManager();
            Map<Integer, Boolean> access = new HashMap<>();
            List<Candidate> candidates = new ArrayList<>();
            int radiusSquared = radius * radius;
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radiusSquared) {
                        continue;
                    }
                    int x = eyeX + dx;
                    int z = eyeZ + dz;
                    // looking up an unloaded chunk would load its reinforcements from the database
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    for (int dy = -radius; dy <= radius; dy++) {
                        int y = eyeY + dy;
                        int distanceSquared = dx * dx + dy * dy + dz * dz;
                        if (y < minY || y >= maxY || distanceSquared > radiusSquared) {
                            continue;
                        }
                        Reinforcement rein = reinforcements.getReinforcement(new Location(world, x, y, z));
                        if (rein == null || rein.isBroken()) {
                            continue;
                        }
                        Block block = world.getBlockAt(x, y, z);
                        LabelText text = null;
                        for (BlockFace side : SIDES) {
                            // the eye has to be in front of the face, a face seen edge on or from behind is skipped
                            if (side.getModX() * -dx + side.getModY() * -dy + side.getModZ() * -dz <= 0) {
                                continue;
                            }
                            if (block.getRelative(side).getType().isOccluding()) {
                                continue;
                            }
                            if (text == null) {
                                text = labelText(player, rein, access);
                            }
                            candidates.add(new Candidate(new Face(x, y, z, side), distanceSquared, block, text));
                        }
                    }
                }
            }
            if (candidates.size() > MAX_LABELS) {
                candidates.sort(Comparator.comparingInt(Candidate::distanceSquared));
                candidates = candidates.subList(0, MAX_LABELS);
            }

            List<Integer> destroy = new ArrayList<>();
            Map<Face, Label> kept = new HashMap<>();
            for (Candidate candidate : candidates) {
                Placement placement = place(candidate.block(), candidate.face().side(), facing);
                Content content = fit(candidate.text(), placement);
                Label label = labels.remove(candidate.face());
                if (label != null && !label.placement.equals(placement)) {
                    destroy.add(label.entityId);
                    label = null;
                }
                if (label == null) {
                    label = new Label(Bukkit.getUnsafe().nextEntityId(world), placement, content);
                    spawn(player, label);
                } else if (!label.content.equals(content)) {
                    label.content = content;
                    sendContent(player, label.entityId, content, false);
                }
                kept.put(candidate.face(), label);
            }
            for (Label stale : labels.values()) {
                destroy.add(stale.entityId);
            }
            labels.clear();
            labels.putAll(kept);
            destroy(player, destroy);
        }

        /** Keeps the shown labels current between scans, health changes far more often than the blocks do. */
        private void refresh(Player player) {
            ReinforcementManager reinforcements = Citadel.getInstance().getReinforcementManager();
            World world = player.getWorld();
            Map<Integer, Boolean> access = new HashMap<>();
            // faces of one block share their text
            Map<Long, LabelText> texts = new HashMap<>();
            List<Integer> destroy = new ArrayList<>();
            for (Iterator<Map.Entry<Face, Label>> iter = labels.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<Face, Label> entry = iter.next();
                Face face = entry.getKey();
                Label label = entry.getValue();
                Reinforcement rein = reinforcements.getReinforcement(new Location(world, face.x(), face.y(), face.z()));
                if (rein == null || rein.isBroken()) {
                    destroy.add(label.entityId);
                    iter.remove();
                    continue;
                }
                LabelText text = texts.computeIfAbsent(Block.getBlockKey(face.x(), face.y(), face.z()),
                    key -> labelText(player, rein, access));
                Content content = fit(text, label.placement);
                if (!label.content.equals(content)) {
                    label.content = content;
                    sendContent(player, label.entityId, content, false);
                }
            }
            destroy(player, destroy);
        }

        void destroyAll(Player player) {
            List<Integer> ids = new ArrayList<>();
            for (Label label : labels.values()) {
                ids.add(label.entityId);
            }
            labels.clear();
            destroy(player, ids);
        }
    }

    private static LabelText labelText(Player player, Reinforcement rein, Map<Integer, Boolean> access) {
        boolean friendly = access.computeIfAbsent(rein.getGroupId(),
            id -> rein.hasPermission(player, CitadelPermissionHandler.getInfo()));
        Group group = friendly ? rein.getGroup() : null;
        String name = group != null ? group.getName() : "(hostile)";
        String health = HEALTH.format(rein.getHealth()) + "/" + HEALTH.format(rein.getType().getHealth());
        Component sprite = CivText.icon(rein.getType().getItem().getType());
        TextComponent.Builder healthLine = Component.text();
        int healthWidth = textWidth(health);
        if (sprite != null) {
            healthLine.append(sprite).append(Component.space());
            healthWidth += SPRITE_PIXELS + textWidth(" ");
        }
        healthLine.append(Component.text(health, ModeListener.healthColor(rein)));
        Component component = Component.text()
            .append(Component.text(name, group != null ? CivText.GROUP : CivText.ERROR))
            .appendNewline()
            .append(healthLine)
            .build();
        return new LabelText(component, Math.max(textWidth(name), healthWidth), 2);
    }

    /** Shrinks the text until it fits its face, and centers it vertically, since a display grows up from its origin. */
    private static Content fit(LabelText text, Placement placement) {
        double width = text.widthPixels() * PIXEL;
        double height = text.lines() * LINE_PIXELS * PIXEL;
        float scale = (float) Math.min(MAX_SCALE,
            Math.min(placement.width() * FILL / width, placement.height() * FILL / height));
        return new Content(text.component(), scale, (float) (-height * scale / 2));
    }

    /**
     * Text displays face south at yaw 0. Labels on top and bottom faces are turned so their text reads upright for
     * a player looking the way they currently face.
     */
    private static Placement place(Block block, BlockFace side, BlockFace facing) {
        BoundingBox box = block.getBoundingBox();
        if (box.getVolume() <= 0) {
            box = BoundingBox.of(block);
        }
        double x = box.getCenterX();
        double y = box.getCenterY();
        double z = box.getCenterZ();
        float flatYaw = yaw(facing.getOppositeFace());
        boolean flatAlongX = facing.getModZ() != 0;
        double flatWidth = flatAlongX ? box.getWidthX() : box.getWidthZ();
        double flatHeight = flatAlongX ? box.getWidthZ() : box.getWidthX();
        return switch (side) {
            case NORTH -> new Placement(x, y, box.getMinZ() - FACE_OFFSET, yaw(side), 0, box.getWidthX(), box.getHeight());
            case SOUTH -> new Placement(x, y, box.getMaxZ() + FACE_OFFSET, yaw(side), 0, box.getWidthX(), box.getHeight());
            case WEST -> new Placement(box.getMinX() - FACE_OFFSET, y, z, yaw(side), 0, box.getWidthZ(), box.getHeight());
            case EAST -> new Placement(box.getMaxX() + FACE_OFFSET, y, z, yaw(side), 0, box.getWidthZ(), box.getHeight());
            case UP -> new Placement(x, box.getMaxY() + FACE_OFFSET, z, flatYaw, -90, flatWidth, flatHeight);
            case DOWN -> new Placement(x, box.getMinY() - FACE_OFFSET, z, flatYaw, 90, flatWidth, flatHeight);
            default -> throw new IllegalArgumentException("Not a block side: " + side);
        };
    }

    private static float yaw(BlockFace direction) {
        return switch (direction) {
            case SOUTH -> 0;
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> -90;
            default -> throw new IllegalArgumentException("Not a horizontal direction: " + direction);
        };
    }

    /** Approximate advance of the default font, close enough to size a label. */
    private static int textWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += switch (text.charAt(i)) {
                case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 2;
                case 'l', '`' -> 3;
                case 't', 'I', ' ', '[', ']', '"', '*', '{', '}' -> 4;
                case 'f', 'k', '(', ')', '<', '>' -> 5;
                case '@', '~' -> 7;
                default -> 6;
            };
        }
        return width;
    }

    private static void spawn(Player player, Label label) {
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        Placement placement = label.placement;
        PacketContainer spawn = protocol.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers()
            .write(0, label.entityId)
            .write(1, 0);
        spawn.getModifier().withType(Vec3.class)
            .write(0, Vec3.ZERO);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getEntityTypeModifier()
            .write(0, EntityType.TEXT_DISPLAY);
        spawn.getDoubles()
            .write(0, placement.x())
            .write(1, placement.y())
            .write(2, placement.z());
        spawn.getBytes()
            .write(0, angle(placement.pitch()))
            .write(1, angle(placement.yaw()))
            .write(2, angle(placement.yaw()));
        protocol.sendServerPacket(player, spawn);
        sendContent(player, label.entityId, label.content, true);
    }

    private static void sendContent(Player player, int entityId, Content content, boolean initial) {
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        PacketContainer metadata = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, entityId);
        List<WrappedDataValue> values = new ArrayList<>();
        // translation, applied in the label's own frame, so it shifts along the text and not the world
        values.add(new WrappedDataValue(11, VECTOR_SERIALIZER, new Vector3f(0, content.shift(), 0)));
        values.add(new WrappedDataValue(12, VECTOR_SERIALIZER,
            new Vector3f(content.scale(), content.scale(), content.scale())));
        if (initial) {
            // full brightness, labels inside a dark base stay readable
            values.add(new WrappedDataValue(16, WrappedDataWatcher.Registry.get(Integer.class), FULL_BRIGHT));
        }
        values.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(),
            new AdventureComponent(content.component())));
        if (initial) {
            values.add(new WrappedDataValue(24, WrappedDataWatcher.Registry.get(Integer.class), LINE_WIDTH));
            // transparent background, the default is a dark box
            values.add(new WrappedDataValue(25, WrappedDataWatcher.Registry.get(Integer.class), 0));
        }
        metadata.getDataValueCollectionModifier().write(0, values);
        protocol.sendServerPacket(player, metadata);
    }

    private static void destroy(Player player, List<Integer> entityIds) {
        if (entityIds.isEmpty()) {
            return;
        }
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        PacketContainer destroy = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, entityIds);
        protocol.sendServerPacket(player, destroy);
    }

    private static byte angle(float degrees) {
        return (byte) Math.floor(degrees * 256.0F / 360.0F);
    }
}
