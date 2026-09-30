package com.untamedears.jukealert.util;

import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.SnitchManager;
import com.untamedears.jukealert.model.Snitch;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.permission.PermissionType;

// Static methods only
public final class JAUtility {

    private JAUtility() {

    }

    private static double tanPiDiv = Math.sqrt(2.0) - 1.0;

    public static final int MAX_NAME_LENGTH = 40;

    public static Snitch findClosestSnitch(Location loc, PermissionType perm, UUID player) {
        Snitch closestSnitch = null;
        double closestDistance = Double.MAX_VALUE;
        Collection<Snitch> snitches = JukeAlert.getInstance().getSnitchManager().getSnitchesCovering(loc);
        for (Snitch snitch : snitches) {
            if (snitch.hasPermission(player, perm)) {
                double distance = snitch.getLocation().distanceSquared(loc);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestSnitch = snitch;
                }
            }
        }
        return closestSnitch;
    }

    public static Snitch findLookingAtOrClosestSnitch(Player player, PermissionType perm) {

        Snitch cursorSnitch = getSnitchUnderCursor(player);
        if (cursorSnitch != null && cursorSnitch.hasPermission(player, perm)) {
            return cursorSnitch;
        }
        return findClosestSnitch(player.getLocation(), perm, player.getUniqueId());
    }

    public static Snitch getSnitchUnderCursor(Player player) {
        SnitchManager snitchMan = JukeAlert.getInstance().getSnitchManager();
        Iterator<Block> itr = new BlockIterator(player, 40); // Within 2.5 chunks
        while (itr.hasNext()) {
            Block block = itr.next();
            Snitch found = snitchMan.getSnitchAt(block.getLocation());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Renames the snitch, cut to the length limit, and tells the player */
    public static void renameSnitch(Player player, Snitch snitch, String name) {
        String newName = name.strip();
        if (newName.length() > MAX_NAME_LENGTH) {
            newName = newName.substring(0, MAX_NAME_LENGTH);
        }
        String prevName = snitch.getName();
        JukeAlert.getInstance().getSnitchManager().renameSnitch(snitch, newName);
        if (prevName.isEmpty()) {
            CivUI.success(player, "Named this ", CivText.subject(snitch.getType().getName()), " ", JAText.snitch(snitch));
        } else {
            CivUI.success(player, "Renamed ", CivText.subject(prevName), " to ", JAText.snitch(snitch));
        }
    }

    public static String getCardinal(Location start, Location end) {
        double dX = start.getBlockX() - end.getBlockX();
        double dZ = start.getBlockZ() - end.getBlockZ();

        if (Math.abs(dX) > Math.abs(dZ)) {
            if (Math.abs(dZ / dX) <= tanPiDiv) {
                return dX > 0 ? "West" : "East";
            } else if (dX > 0) {
                return dZ > 0 ? "North West" : "South West";
            } else {
                return dZ > 0 ? "North East" : "South East";
            }
        } else if (Math.abs(dZ) > 0) {
            if (Math.abs(dX / dZ) <= tanPiDiv) {
                return dZ > 0 ? "North" : "South";
            } else if (dZ > 0) {
                return dX > 0 ? "North West" : "North East";
            } else {
                return dX > 0 ? "South West" : "South East";
            }
        } else {
            return "";
        }
    }

    public static Material parseMaterial(String id) {
        try {
            return Material.valueOf(id);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public static boolean isSameWorld(Location loc1, Location loc2) {
        return loc1.getWorld().getUID().equals(loc2.getWorld().getUID());
    }

    public static String formatLocation(Location location, boolean includeWorld) {
        if (includeWorld) {
            return String.format("[%s %d %d %d]", location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        }
        return String.format("[%d %d %d]", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Resolves the recorded name of a vehicle or mount into an item to represent it in the snitch log GUI.
     *
     * <p>Nearly every entity type lines up with either a spawn egg ({@code HORSE} -> {@code HORSE_SPAWN_EGG}) or an
     * item of the same name ({@code CHEST_MINECART}, {@code OAK_BOAT}), so those are resolved by name rather than
     * listed one by one. New mounts and boat types therefore work without a code change here; only names that follow
     * neither pattern need an explicit mapping.
     *
     * @param vehicle Recorded name of the vehicle or mount, which may also be a custom entity name
     * @return Material to show for it, or {@link Material#SADDLE} if the name can't be resolved
     */
    public static Material getVehicle(String vehicle) {
        switch (vehicle) {
            // Logs written before the wood specific boat types existed stored every boat under these names
            case "BOAT":
                return Material.OAK_BOAT;
            case "CHEST_BOAT":
                return Material.OAK_CHEST_BOAT;
            // Cushions only exist as dyed items, so one of them stands in for all of them
            case "CUSHION":
                return Material.WHITE_CUSHION;
            default:
                break;
        }
        Material spawnEgg = Material.getMaterial(vehicle + "_SPAWN_EGG");
        if (spawnEgg != null) {
            return spawnEgg;
        }
        Material item = Material.getMaterial(vehicle);
        if (item != null) {
            return item;
        }
        JukeAlert.getInstance().getLogger().info("Failed to parse vehicle into material: " + vehicle);
        return Material.SADDLE;
    }
}
