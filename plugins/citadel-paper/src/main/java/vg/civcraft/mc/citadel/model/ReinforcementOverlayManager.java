package vg.civcraft.mc.citadel.model;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.papermc.paper.adventure.AdventureComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import vg.civcraft.mc.citadel.CitadelConfigManager;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.events.ReinforcementCreationEvent;
import vg.civcraft.mc.citadel.listener.ModeListener;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.group.Group;

/**
 * Labels the exposed faces of reinforced blocks around a player with group and health. Each label is a text
 * display that only exists on that player's client, so the server pays in packets, not entities.
 */
public class ReinforcementOverlayManager implements Listener {

    /** Each player is updated once per this many ticks, in their own slot so the load spreads over every tick. */
    private static final int UPDATE_INTERVAL_TICKS = 4;
    /** Time scans may take per tick, the rest wait for the next tick so many moving players cannot stall the server. */
    private static final long SCAN_BUDGET_NANOS = 2_000_000L;
    /** Matches the setting's upper bound, and also caps values saved back when the bound was higher. */
    private static final int MAX_RADIUS = 6;
    /** Updates between full scans for a player standing still, which pick up blocks changed by anything else. */
    private static final int RESCAN_UPDATES = 10;
    /** Closest faces win past this, so a wall of reinforced blocks cannot flood a client with entities. */
    private static final int MAX_LABELS = 512;
    /** Lifts a label off its face so it does not z-fight with the block texture. */
    private static final double FACE_OFFSET = 0.01;
    /** Share of a face a label may cover. */
    private static final double FILL = 0.85;
    /** Share of a face a neighbor has to sit on for the face to count as hidden. */
    private static final double COVERED_SHARE = 0.5;
    /** Points per axis sampled across a face to measure how much of it a neighbor covers. */
    private static final int COVER_SAMPLES = 4;
    private static final double EPSILON = 1.0E-4;
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
    private static final WrappedDataWatcher.Serializer VECTOR_SERIALIZER =
        new WrappedDataWatcher.Serializer(Vector3fc.class, EntityDataSerializers.VECTOR3, false);

    private final CitadelSettingManager settings;
    private final CitadelConfigManager  configManager;
    private final Map<UUID, PlayerOverlay> overlays = new HashMap<>();
    // a player moving or blocks changing near them goes ahead of the periodic rescan of a player standing still
    private final Deque<UUID> urgentScans = new ArrayDeque<>();
    private final Deque<UUID> periodicScans = new ArrayDeque<>();
    private int tickCount;
    private int nextSlot;

    public ReinforcementOverlayManager(CitadelSettingManager settings) {
        this.settings = settings;
        this.configManager = Citadel.getInstance().getConfigManager();
        settings.getOverlay().registerListener((uuid, setting, oldValue, newValue) -> {
            if (!newValue) {
                clear(Bukkit.getPlayer(uuid));
            }
        });
        configManager.getCtvEnabled().registerListener(((setting, oldValue, newValue) -> {
            if (!newValue) {
                Bukkit.getOnlinePlayers().forEach(this::clear);
            }
        }));
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!configManager.getCtvEnabled().getValue()){
                    return;
                }
                tick();
            }

        }.runTaskTimer(Citadel.getInstance(), 1L, 1L);
    }

    /** Removes every label from every client, for shutdown and reload. */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(overlays.keySet())) {
            clear(Bukkit.getPlayer(uuid));
        }
        overlays.clear();
        urgentScans.clear();
        periodicScans.clear();
    }

    private void tick() {
        int slot = tickCount++ % UPDATE_INTERVAL_TICKS;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!settings.getOverlay().getValue(player.getUniqueId()) || Bedrock.isBedrock(player)) {
                continue;
            }
            PlayerOverlay overlay = overlays.computeIfAbsent(player.getUniqueId(),
                k -> new PlayerOverlay(nextSlot++ % UPDATE_INTERVAL_TICKS));
            if (overlay.slot == slot) {
                overlay.update(player);
            }
        }
        runScans();
    }

    private void runScans() {
        long start = System.nanoTime();
        while (System.nanoTime() - start < SCAN_BUDGET_NANOS) {
            Queued kind = Queued.URGENT;
            UUID uuid = urgentScans.poll();
            if (uuid == null) {
                kind = Queued.PERIODIC;
                uuid = periodicScans.poll();
                if (uuid == null) {
                    return;
                }
            }
            PlayerOverlay overlay = overlays.get(uuid);
            Player player = Bukkit.getPlayer(uuid);
            // a player moved up to urgent leaves a stale entry in the periodic queue, only the matching one counts
            if (overlay == null || player == null || overlay.queued != kind) {
                continue;
            }
            overlay.scan(player);
        }
    }

    private void enqueue(UUID uuid, PlayerOverlay overlay, Queued kind) {
        if (overlay.queued == Queued.URGENT || overlay.queued == kind) {
            return;
        }
        overlay.queued = kind;
        (kind == Queued.URGENT ? urgentScans : periodicScans).add(uuid);
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

    private enum Queued {
        NONE, PERIODIC, URGENT
    }

    private record Face(int x, int y, int z, BlockFace side) {
    }

    /** Where a label sits and how much room its face offers. */
    private record Placement(double x, double y, double z, float yaw, float pitch, double width, double height) {
    }

    /**
     * Everything a label shows, as plain values. Comparing these is all a refresh does for an unchanged label, the
     * text is only built when they differ.
     */
    private record LabelState(int health, int maxHealth, short typeId, int groupId, String groupName,
                              boolean friendly, int background) {
    }

    private record LabelText(Component component, int widthPixels, int lines) {
    }

    private record Content(Component component, float scale, float shift, int background) {
    }

    private record Candidate(Face face, int distanceSquared, Block block, Reinforcement reinforcement,
                             LabelState state) {
    }

    private static final class Label {

        private final int entityId;
        private final Placement placement;
        private Reinforcement reinforcement;
        private LabelState state;
        private Content content;

        private Label(int entityId, Placement placement, Reinforcement reinforcement, LabelState state,
                      Content content) {
            this.entityId = entityId;
            this.placement = placement;
            this.reinforcement = reinforcement;
            this.state = state;
            this.content = content;
        }
    }

    private final class PlayerOverlay {

        private final int slot;
        private final Map<Face, Label> labels = new HashMap<>();
        private UUID worldId;
        private int eyeX;
        private int eyeY;
        private int eyeZ;
        private int radius;
        /** ARGB of the box behind the text, read every update so a changed setting reaches the shown labels. */
        private int background;
        private BlockFace facing;
        private int updatesSinceScan;
        private boolean dirty = true;
        private Queued queued = Queued.NONE;

        PlayerOverlay(int slot) {
            this.slot = slot;
        }

        void update(Player player) {
            Location eye = player.getEyeLocation();
            int opacity = settings.getOverlayBackgroundOpacity().getValue(player.getUniqueId());
            background = Math.round(opacity * 255 / 100.0F) << 24;
            boolean moved = !eye.getWorld().getUID().equals(worldId) || eye.getBlockX() != eyeX
                || eye.getBlockY() != eyeY || eye.getBlockZ() != eyeZ || radius(player) != radius
                || player.getFacing() != facing;
            if (dirty || moved) {
                enqueue(player.getUniqueId(), this, Queued.URGENT);
            } else if (++updatesSinceScan >= RESCAN_UPDATES) {
                enqueue(player.getUniqueId(), this, Queued.PERIODIC);
            }
            // a queued scan may wait a few ticks, the shown labels keep their health current meanwhile
            refresh(player);
        }

        private int radius(Player player) {
            return Math.min(MAX_RADIUS, settings.getOverlayRadius().getValue(player.getUniqueId()));
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

        void scan(Player player) {
            Location eye = player.getEyeLocation();
            World world = eye.getWorld();
            worldId = world.getUID();
            eyeX = eye.getBlockX();
            eyeY = eye.getBlockY();
            eyeZ = eye.getBlockZ();
            radius = radius(player);
            facing = player.getFacing();
            updatesSinceScan = 0;
            dirty = false;
            queued = Queued.NONE;

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
                        // reinforced air is a leftover that Citadel only cleans up once something is placed there
                        if (block.getType().isAir()) {
                            continue;
                        }
                        LabelState state = null;
                        for (BlockFace side : SIDES) {
                            // the eye has to be in front of the face, a face seen edge on or from behind is skipped
                            if (side.getModX() * -dx + side.getModY() * -dy + side.getModZ() * -dz <= 0) {
                                continue;
                            }
                            if (isCovered(block, side)) {
                                continue;
                            }
                            if (state == null) {
                                boolean friendly = access.computeIfAbsent(rein.getGroupId(),
                                    id -> rein.hasPermission(player, CitadelPermissionHandler.getInfo()));
                                state = labelState(rein, friendly, background);
                            }
                            candidates.add(new Candidate(new Face(x, y, z, side), distanceSquared, block, rein, state));
                        }
                    }
                }
            }
            if (candidates.size() > MAX_LABELS) {
                candidates.sort(Comparator.comparingInt(Candidate::distanceSquared));
                candidates = candidates.subList(0, MAX_LABELS);
            }

            // faces of one block share their text
            Map<Reinforcement, LabelText> texts = new IdentityHashMap<>();
            List<Integer> destroy = new ArrayList<>();
            Map<Face, Label> kept = new HashMap<>();
            for (Candidate candidate : candidates) {
                Placement placement = place(candidate.block(), candidate.face().side(), facing);
                Label label = labels.remove(candidate.face());
                if (label != null && !label.placement.equals(placement)) {
                    destroy.add(label.entityId);
                    label = null;
                }
                if (label == null) {
                    LabelText text = texts.computeIfAbsent(candidate.reinforcement(),
                        rein -> labelText(rein, candidate.state()));
                    label = new Label(Bukkit.getUnsafe().nextEntityId(world), placement, candidate.reinforcement(),
                        candidate.state(), fit(text, placement, background));
                    spawn(player, label);
                } else {
                    // a reloaded chunk hands out new reinforcement objects, the refresh has to follow the current one
                    label.reinforcement = candidate.reinforcement();
                    if (!label.state.equals(candidate.state())) {
                        LabelText text = texts.computeIfAbsent(candidate.reinforcement(),
                            rein -> labelText(rein, candidate.state()));
                        relabel(player, label, candidate.state(), text);
                    }
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

        /**
         * Keeps the shown labels current between scans. It reads the held reinforcements instead of looking them up
         * and only builds text for labels whose values changed. Access is left to the scans, so a reinforcement moved
         * to another group shows as hostile until the next one.
         */
        private void refresh(Player player) {
            Map<Reinforcement, LabelText> texts = new IdentityHashMap<>();
            List<Integer> destroy = new ArrayList<>();
            for (Iterator<Label> iter = labels.values().iterator(); iter.hasNext(); ) {
                Label label = iter.next();
                Reinforcement rein = label.reinforcement;
                if (rein.isBroken()) {
                    destroy.add(label.entityId);
                    iter.remove();
                    continue;
                }
                LabelState old = label.state;
                if (Math.round(rein.getHealth()) == old.health() && rein.getType().getID() == old.typeId()
                    && rein.getGroupId() == old.groupId() && background == old.background()) {
                    continue;
                }
                boolean friendly = old.friendly() && rein.getGroupId() == old.groupId();
                LabelState state = labelState(rein, friendly, background);
                relabel(player, label, state, texts.computeIfAbsent(rein, r -> labelText(r, state)));
            }
            destroy(player, destroy);
        }

        private void relabel(Player player, Label label, LabelState state, LabelText text) {
            label.state = state;
            Content content = fit(text, label.placement, state.background());
            if (!content.equals(label.content)) {
                label.content = content;
                sendContent(player, label.entityId, content, false);
            }
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

    /**
     * Whether the neighbor hides the face. Glass and leaves do not occlude, and two stacked fences only meet at their
     * posts, so instead of trusting the material this samples the face and checks how much of it lies against the
     * neighbor's shape.
     */
    private static boolean isCovered(Block block, BlockFace side) {
        Block neighbor = block.getRelative(side);
        if (neighbor.getType().isOccluding()) {
            return true;
        }
        BoundingBox box = shape(block);
        double[] min = {box.getMinX() - block.getX(), box.getMinY() - block.getY(), box.getMinZ() - block.getZ()};
        double[] max = {box.getMaxX() - block.getX(), box.getMaxY() - block.getY(), box.getMaxZ() - block.getZ()};
        int axis = side.getModX() != 0 ? 0 : side.getModY() != 0 ? 1 : 2;
        boolean positive = side.getModX() + side.getModY() + side.getModZ() > 0;
        // a face set back from the block edge, like the top of a bottom slab, keeps a gap to its neighbor
        if (positive ? max[axis] < 1 - EPSILON : min[axis] > EPSILON) {
            return false;
        }
        List<double[][]> touching = new ArrayList<>();
        for (BoundingBox part : neighbor.getCollisionShape().getBoundingBoxes()) {
            double[] partMin = {part.getMinX(), part.getMinY(), part.getMinZ()};
            double[] partMax = {part.getMaxX(), part.getMaxY(), part.getMaxZ()};
            if (positive ? partMin[axis] <= EPSILON : partMax[axis] >= 1 - EPSILON) {
                touching.add(new double[][] {partMin, partMax});
            }
        }
        if (touching.isEmpty()) {
            return false;
        }
        int u = axis == 0 ? 1 : 0;
        int v = axis == 2 ? 1 : 2;
        int covered = 0;
        for (int i = 0; i < COVER_SAMPLES; i++) {
            double pu = min[u] + (i + 0.5) / COVER_SAMPLES * (max[u] - min[u]);
            for (int j = 0; j < COVER_SAMPLES; j++) {
                double pv = min[v] + (j + 0.5) / COVER_SAMPLES * (max[v] - min[v]);
                for (double[][] part : touching) {
                    if (pu >= part[0][u] - EPSILON && pu <= part[1][u] + EPSILON
                        && pv >= part[0][v] - EPSILON && pv <= part[1][v] + EPSILON) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return covered >= COVERED_SHARE * COVER_SAMPLES * COVER_SAMPLES;
    }

    /** The block's outline, or the full cube for blocks without one. */
    private static BoundingBox shape(Block block) {
        BoundingBox box = block.getBoundingBox();
        return box.getVolume() <= 0 ? BoundingBox.of(block) : box;
    }

    private static LabelState labelState(Reinforcement rein, boolean friendly, int background) {
        Group group = friendly ? rein.getGroup() : null;
        return new LabelState(Math.round(rein.getHealth()), Math.round(rein.getType().getHealth()),
            rein.getType().getID(), rein.getGroupId(), group != null ? group.getName() : null, group != null,
            background);
    }

    private static LabelText labelText(Reinforcement rein, LabelState state) {
        String name = state.friendly() ? state.groupName() : "(hostile)";
        String health = state.health() + "/" + state.maxHealth();
        Component sprite = CivText.icon(rein.getType().getItem().getType());
        TextComponent.Builder healthLine = Component.text();
        int healthWidth = textWidth(health);
        if (sprite != null) {
            healthLine.append(sprite).append(Component.space());
            healthWidth += SPRITE_PIXELS + textWidth(" ");
        }
        healthLine.append(Component.text(health, ModeListener.healthColor(rein)));
        Component component = Component.text()
            .append(Component.text(name, state.friendly() ? CivText.GROUP : CivText.ERROR))
            .appendNewline()
            .append(healthLine)
            .build();
        return new LabelText(component, Math.max(textWidth(name), healthWidth), 2);
    }

    /** Shrinks the text until it fits its face, and centers it vertically, since a display grows up from its origin. */
    private static Content fit(LabelText text, Placement placement, int background) {
        double width = text.widthPixels() * PIXEL;
        double height = text.lines() * LINE_PIXELS * PIXEL;
        float scale = (float) Math.min(MAX_SCALE,
            Math.min(placement.width() * FILL / width, placement.height() * FILL / height));
        return new Content(text.component(), scale, (float) (-height * scale / 2), background);
    }

    /**
     * Text displays face south at yaw 0. Labels on top and bottom faces are turned so their text reads upright for
     * a player looking the way they currently face.
     */
    private static Placement place(Block block, BlockFace side, BlockFace facing) {
        BoundingBox box = shape(block);
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
        }
        values.add(new WrappedDataValue(25, WrappedDataWatcher.Registry.get(Integer.class), content.background()));
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
