package com.programmerdan.minecraft.simpleadminhacks.listeners;

import org.bukkit.entity.Cushion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CushionListener implements Listener {

    /**
     * Stops a player who is already sitting on a cushion from sitting on another one
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void TTCushionGeniusFunction(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Cushion
            && isSittingOnOtherCushion(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void TTCushionGeniusFunction(EntityMountEvent event) {
        if (event.getMount() instanceof Cushion && event.getEntity() instanceof Player player
            && isSittingOnOtherCushion(player, event.getMount())) {
            event.setCancelled(true);
        }
    }

    private static boolean isSittingOnOtherCushion(Player player, Entity target) {
        Entity vehicle = player.getVehicle();
        return vehicle instanceof Cushion && !vehicle.equals(target);
    }
}
