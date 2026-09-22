package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.PacketManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityMountEvent;

public final class AttrHider extends BasicHack {

    public static final String BYPASS_PERMISSION = "attrhider.bypass";

    private final PacketManager packets = new PacketManager();
    private Set<Object> customPackets = new HashSet<>();

    @AutoLoad
    private boolean hideEffects;

    @AutoLoad
    private boolean hideHealth;

    @AutoLoad
    private boolean roundPlayerListPing;

    public AttrHider(final SimpleAdminHacks plugin, final BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (this.hideEffects) {
            this.packets.addAdapter(new PacketAdapter(this.plugin, PacketType.Play.Server.ENTITY_EFFECT) {
                @Override
                public void onPacketSending(final PacketEvent event) {
                    final PacketContainer packet = event.getPacket();
                    final Player player = event.getPlayer();
                    if (player.hasPermission(BYPASS_PERMISSION)) {
                        return;
                    }
                    if (player.getEntityId() == packet.getIntegers().read(0)) {
                        return;
                    }
                    // A deep clone would copy the effect's registry holder too, and holders are
                    // resolved back to a network id by identity when the packet is encoded, so a
                    // copy cannot be serialised and the connection gets closed. Only the fields
                    // overwritten below have to be ours, so a shallow copy is enough.
                    final PacketContainer cloned = packet.shallowClone();
                    final StructureModifier<Integer> ints = cloned.getIntegers();
                    // set amplifier to 0
                    cloned.getBytes().write(1, (byte) 0);
                    // set duration to 0
                    ints.write(1, 0);
                    // The packet data is shared between events, but the event
                    // instance is exclusive to THIS sending of the packet
                    event.setPacket(cloned);
                }
            });
        }
        if (this.hideHealth) {
            this.packets.addAdapter(new PacketAdapter(this.plugin, PacketType.Play.Server.ENTITY_METADATA) {
                @Override
                public void onPacketSending(final PacketEvent event) {
                    final PacketContainer packet = event.getPacket();
                    final Player player = event.getPlayer();
                    if (player.hasPermission(BYPASS_PERMISSION)) {
                        return;
                    }
                    final Entity entity = packet.getEntityModifier(event).read(0);
                    if (!(entity instanceof LivingEntity)
                        || player.getEntityId() == entity.getEntityId()
                        || entity.getPassengers().contains(player)
                        || customPackets.remove(packet.getHandle())) {
                        return;
                    }
                    final List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(0);
                    if (values == null) {
                        return;
                    }
                    // Replace only the entries we actually change and leave the rest of the list
                    // alone. Deep cloning the packet would also copy registry backed values such
                    // as entity variants, and those are resolved back to a network id by identity
                    // when the packet is encoded, so a copy cannot be serialised and the
                    // connection gets closed.
                    List<WrappedDataValue> hidden = null;
                    for (int i = 0; i < values.size(); i++) {
                        final WrappedDataValue object = values.get(i);
                        // Read the 8th field as a float as that's the living entity's health
                        // https://wiki.vg/Entity_metadata#Living_Entity
                        if (object == null || object.getIndex() != 9
                            || !(object.getValue() instanceof Float health) || health <= 0f) {
                            continue;
                        }
                        if (hidden == null) {
                            hidden = new ArrayList<>(values);
                        }
                        // Half a heart
                        hidden.set(i, new WrappedDataValue(9, WrappedDataWatcher.Registry.get(Float.class), 1f));
                    }
                    if (hidden == null) {
                        return;
                    }
                    // The packet data is shared between events, but the event
                    // instance is exclusive to THIS sending of the packet
                    final PacketContainer cloned = packet.shallowClone();
                    cloned.getDataValueCollectionModifier().write(0, hidden);
                    event.setPacket(cloned);
                }
            });
        }
        if (this.roundPlayerListPing) {
            this.packets.addAdapter(new PacketAdapter(this.plugin, PacketType.Play.Server.PLAYER_INFO) {
                @Override
                public void onPacketSending(final PacketEvent event) {
                    final PacketContainer packet = event.getPacket();
                    final Player player = event.getPlayer();
                    if (player.hasPermission(BYPASS_PERMISSION)) {
                        return;
                    }
                    final StructureModifier<List<PlayerInfoData>> infoLists = packet.getPlayerInfoDataLists();
                    final int entriesIndex = findEntriesIndex(infoLists);
                    if (entriesIndex < 0) {
                        return;
                    }
                    List<PlayerInfoData> oldInfos = infoLists.read(entriesIndex);
                    if (oldInfos == null) {
                        return;
                    }
                    // Shallow copy: the entries are rebuilt from scratch below, and deep cloning
                    // profiles and chat session data is both wasteful and unsafe for any value
                    // the encoder resolves by identity.
                    final PacketContainer cloned = packet.shallowClone();
                    List<PlayerInfoData> newInfos = new ArrayList<>();
                    for (PlayerInfoData oldInfo : oldInfos) {
                        if (oldInfo == null) continue;
                        int latency = oldInfo.getLatency();
                        // Limit player ping in the tablist to the same 6 values vanilla clients can discern visually
                        // this follows 1.16.5 PlayerTabOverlay#renderPingIcon()
                        if (latency < 0) latency = -1;
                        else if (latency < 150) latency = 75; // average of 0 and 150, arbitrary
                        else if (latency < 300) latency = 225;
                        else if (latency < 600) latency = 450;
                        else if (latency < 1000) latency = 800;
                        else latency = 1000;
                        newInfos.add(new PlayerInfoData(
                            oldInfo.getProfileId(),
                            latency,
                            oldInfo.isListed(),
                            oldInfo.getGameMode(),
                            oldInfo.getProfile(),
                            oldInfo.getDisplayName(),
                            oldInfo.getRemoteChatSessionData()
                        ));
                    }
                    cloned.getPlayerInfoDataLists().write(entriesIndex, newInfos);
                    // The packet data is shared between events, but the event
                    // instance is exclusive to THIS sending of the packet
                    event.setPacket(cloned);
                }
            });
        }
    }

    /**
     * Locates the entry list of a player info update packet. The packet's action set is a
     * collection as well, so whether it takes up one of the collection field slots ProtocolLib
     * hands out depends on the ProtocolLib version; the entry list is the only field declared as
     * a {@link List}, so find it by type instead of assuming a fixed index.
     *
     * @param infoLists the packet's player info lists.
     * @return the field index of the entry list, or -1 if the packet has none.
     */
    private static int findEntriesIndex(final StructureModifier<List<PlayerInfoData>> infoLists) {
        final List<FieldAccessor> fields = infoLists.getFields();
        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i).getField();
            if (field != null && List.class.isAssignableFrom(field.getType())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        this.packets.removeAllAdapters();
        super.onDisable();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMountEntity(EntityMountEvent event) {
        if (this.hideHealth && event.getEntity() instanceof Player player && event.getMount() instanceof LivingEntity mountedEntity) {
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();

            watcher.setEntity(mountedEntity);
            watcher.setObject(9, WrappedDataWatcher.Registry.get(Float.class), (float) mountedEntity.getHealth());
            for (WrappedWatchableObject entry : watcher.getWatchableObjects()) {
                if (entry == null) continue;

                WrappedDataWatcher.WrappedDataWatcherObject watcherObject = entry.getWatcherObject();

                wrappedDataValueList.add(new WrappedDataValue(watcherObject.getIndex(), watcherObject.getSerializer(), entry.getRawValue()));
            }

            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, mountedEntity.getEntityId());
            packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            customPackets.add(packet.getHandle()); // Allow ignoring this packet when hiding health because EntityMountEvent fires *before* the player is actually a passenger
        }
    }
}
