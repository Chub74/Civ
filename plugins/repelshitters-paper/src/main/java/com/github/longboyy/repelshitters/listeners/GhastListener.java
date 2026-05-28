package com.github.longboyy.repelshitters.listeners;

import com.github.longboyy.repelshitters.HappyGhastManager;
import com.github.longboyy.repelshitters.RepelShitters;
import io.papermc.paper.event.entity.EntityMoveEvent;
import isaac.bastion.Bastion;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.List;

public class GhastListener implements Listener {

    private final RepelShitters plugin;

    public GhastListener(RepelShitters plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Claims bastion: hard repulsion via movement cancellation
    // Must run at HIGH priority so we cancel before other plugins react.
    // -----------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity().getType() != EntityType.HAPPY_GHAST) return;
        plugin.getHappyGhastManager().handleGhastMoveForClaims(event);
    }

    // -----------------------------------------------------------------------
    // City bastion: instant altitude hard-clamp via setTo()
    // Runs at NORMAL priority (after HIGH claims check) so a cancelled move
    // never reaches this handler unnecessarily.
    // -----------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityMoveCity(EntityMoveEvent event) {
        if (event.getEntity().getType() != EntityType.HAPPY_GHAST) return;
        plugin.getHappyGhastManager().handleGhastMoveForCity(event);
    }

    // -----------------------------------------------------------------------
    // Block mounting a ghast that is already inside enemy claims airspace
    // -----------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        // Stat modification for any ghast mount
        if (event.getEntity().getType() == EntityType.PLAYER
                && event.getMount() instanceof LivingEntity livingEntity) {
            plugin.getHappyGhastManager().modifyGhastStats(livingEntity);
        }

        // Block mounting if the ghast is locked inside enemy claims
        if (!(event.getMount() instanceof HappyGhast ghast)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        HappyGhastManager.GhastBastionTier tier =
                plugin.getHappyGhastManager().classifyHostileTier(ghast);

        if (tier == HappyGhastManager.GhastBastionTier.CLAIMS) {
            event.setCancelled(true);
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(
                            "§c⚡ §lBlocked! §rYou can't mount a ghast inside enemy claims airspace!"));
        }
    }

    // -----------------------------------------------------------------------
    // Apply speed/health config on spawn
    // -----------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        plugin.getHappyGhastManager().modifyGhastStats(livingEntity);

        // Block deploying a new ghast inside enemy claims
        if (event.getEntity().getType() != EntityType.HAPPY_GHAST) return;
        HappyGhast ghast = (HappyGhast) event.getEntity();

        // Use a brief lookup: if the spawn location is inside hostile claims,
        // find nearby players to notify and cancel the spawn.
        var bastions = Bastion.getBastionManager().getBlockingBastions(ghast.getLocation());
        if (bastions.isEmpty()) return;

        // Check nearby players (within 10 blocks) as potential deployers
        List<Player> nearbyPlayers = ghast.getLocation().getWorld()
                .getNearbyEntitiesByType(Player.class, ghast.getLocation(), 10.0)
                .stream()
                .filter(p -> bastions.stream()
                        .anyMatch(b -> !b.canPlace(p)
                                && b.getType().getName().equals(
                                        plugin.getConfigManager().getClaimsBastionName())))
                .toList();

        if (!nearbyPlayers.isEmpty()) {
            event.setCancelled(true);
            for (Player p : nearbyPlayers) {
                p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize(
                                "§c⚡ §lBlocked! §rYou can't deploy a ghast inside enemy claims airspace!"));
            }
        }
    }
}
