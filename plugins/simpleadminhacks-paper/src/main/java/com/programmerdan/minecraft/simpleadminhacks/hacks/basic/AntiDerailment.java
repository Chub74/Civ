package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.craftbukkit.entity.CraftMinecart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prevents minecarts from derailing or stalling at the foot of a slope by forcibly slowing them down to 8m/s when
 * they would otherwise do so
 */
public class AntiDerailment extends BasicHack {

    private static final Set<Rail.Shape> ASCENDING = EnumSet.of(
        Rail.Shape.ASCENDING_NORTH, Rail.Shape.ASCENDING_SOUTH, Rail.Shape.ASCENDING_EAST, Rail.Shape.ASCENDING_WEST
    );

    private Map<Minecart, Vector> previousTickMinecartVelocity;
    private final NamespacedKey ticking = new NamespacedKey(SimpleAdminHacks.instance(), "ticking");

    public AntiDerailment(SimpleAdminHacks plugin, BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        previousTickMinecartVelocity = new HashMap<>();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousTickMinecartVelocity.clear();
    }

    @EventHandler
    public void on(EntityRemoveFromWorldEvent e) {
        if (e.getEntity() instanceof Minecart minecart) {
            previousTickMinecartVelocity.remove(minecart);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDerailment(VehicleMoveEvent e) {
        // This method is EXTREMELY fucked but basically if it detects a super fast minecart is going to derail, it will
        // undo one tick of movement and redo it at 8m/s (the default minecart speed)
        // To do this it also needs to store the velocity from the previous tick to recreate the movement properly

        if (!(e.getVehicle() instanceof Minecart minecart) || minecart.getPersistentDataContainer().has(ticking)) {
            return;
        }

        List<Entity> passengers = minecart.getPassengers();
        if (passengers.size() != 1 || !(passengers.get(0) instanceof Player)) {
            previousTickMinecartVelocity.put(minecart, minecart.getVelocity());
            return;
        }

        if (minecart.getMaxSpeed() == 0.4D || !previousTickMinecartVelocity.containsKey(minecart)) {
            previousTickMinecartVelocity.put(minecart, minecart.getVelocity());
            return;
        }

        Block fromBlock = e.getFrom().getBlock();
        if (!Tag.RAILS.isTagged(fromBlock.getType())) {
            previousTickMinecartVelocity.put(minecart, minecart.getVelocity());
            return;
        }

        AbstractMinecart handle = ((CraftMinecart) minecart).getHandle();
        Block block = e.getTo().getBlock();
        // Vanilla only lifts a cart onto a slope at the start of a tick, so a fast cart can cross the whole slope
        // block in one tick at the lower height, hit the block the next rail sits on and roll back down
        boolean stalledOnSlope = handle.horizontalCollision && block.getBlockData() instanceof Rail rail
            && ASCENDING.contains(rail.getShape());
        if (Tag.RAILS.isTagged(block.getType()) && !stalledOnSlope) {
            previousTickMinecartVelocity.put(minecart, minecart.getVelocity());
            return;
        }

        handle.setPos(e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ());
        minecart.setVelocity(previousTickMinecartVelocity.get(minecart));
        double maxSpeed = minecart.getMaxSpeed();
        minecart.setMaxSpeed(0.4D); // 8m/s, default minecart speed
        minecart.getPersistentDataContainer().set(ticking, PersistentDataType.BOOLEAN, true);
        try {
            handle.tick();
        } finally {
            minecart.getPersistentDataContainer().remove(ticking);
        }
        minecart.setMaxSpeed(maxSpeed);
        previousTickMinecartVelocity.put(minecart, minecart.getVelocity());
    }
}
