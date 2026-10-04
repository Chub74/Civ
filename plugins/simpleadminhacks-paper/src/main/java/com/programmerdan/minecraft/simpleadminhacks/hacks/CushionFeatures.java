package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.CushionFeaturesConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minelink.ctplus.CombatTagPlus;
import net.minelink.ctplus.event.PlayerCombatTagEvent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.ui.Channel;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public final class CushionFeatures extends SimpleHack<CushionFeaturesConfig> implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** A denied mount fires on every click, so one message per second at most. */
    private static final Duration DENY_WINDOW = Duration.ofSeconds(1);

    /** Rays are cast at these heights above the feet, so a dismount can't slip through a one-high gap. */
    private static final double[] RAY_HEIGHTS = {0.5d, 1.5d};

    private final Map<UUID, Long> lastMountTimes = new HashMap<>();
    private final Map<UUID, Location> mountedFrom = new HashMap<>();
    private final Map<UUID, Integer> dismountTicks = new HashMap<>();

    private Listener combatTagListener;

    public CushionFeatures(@NotNull final SimpleAdminHacks plugin,
                           @NotNull final CushionFeaturesConfig config) {
        super(plugin, config);
    }

    public static CushionFeaturesConfig generate(@NotNull final SimpleAdminHacks plugin,
                                                 @NotNull final ConfigurationSection config) {
        return new CushionFeaturesConfig(plugin, config);
    }

    @Override
    public void onEnable() {
        plugin().registerListener(this);
        if (Bukkit.getPluginManager().isPluginEnabled("CombatTagPlus")) {
            this.combatTagListener = new CombatTagListener();
            plugin().registerListener(this.combatTagListener);
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (this.combatTagListener != null) {
            HandlerList.unregisterAll(this.combatTagListener);
            this.combatTagListener = null;
        }
        this.lastMountTimes.clear();
        this.mountedFrom.clear();
        this.dismountTicks.clear();
    }

    private boolean INTERNAL_isInCombat(final Player player) {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        final Plugin foundPlugin = pluginManager.getPlugin("CombatTagPlus");
        if (pluginManager.isPluginEnabled(foundPlugin)) {
            final CombatTagPlus combatTagPlugin = (CombatTagPlus) foundPlugin;
            return combatTagPlugin.getTagManager().isTagged(player.getUniqueId());
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void regulateCushionSitting(@NotNull final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Cushion && denySitting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void regulateCushionSitting(@NotNull final EntityMountEvent event) {
        if (event.getMount() instanceof Cushion && event.getEntity() instanceof Player player
            && denySitting(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rememberCushionSitting(@NotNull final EntityMountEvent event) {
        if (event.getMount() instanceof Cushion && event.getEntity() instanceof Player player) {
            this.lastMountTimes.put(player.getUniqueId(), System.currentTimeMillis());
            this.mountedFrom.put(player.getUniqueId(), player.getLocation());
        }
    }

    /**
     * @return Whether the player may not sit on a cushion right now; tells them why when so.
     */
    private boolean denySitting(final Player player) {
        if (config().isSittingDisabledInCombat() && INTERNAL_isInCombat(player)) {
            sendInCombatDenial(player);
            return true;
        }
        final long cooldown = config().getMountCooldown();
        final Long lastMountTime = this.lastMountTimes.get(player.getUniqueId());
        if (cooldown > 0L && lastMountTime != null) {
            final long remaining = cooldown - (System.currentTimeMillis() - lastMountTime);
            if (remaining > 0L) {
                sendCooldownDenial(player, remaining);
                return true;
            }
        }
        return false;
    }

    private void sendInCombatDenial(final Player player) {
        final String template = config().getInCombatMessage();
        if (template == null || template.isEmpty()) {
            return;
        }
        CivUI.notice(Kind.ERROR, MM.deserialize(template))
            .from(plugin())
            .to(player)
            .atMostEvery(DENY_WINDOW)
            .send();
    }

    private void sendCooldownDenial(final Player player, final long remainingMillis) {
        final String template = config().getCooldownMessage();
        if (template == null || template.isEmpty()) {
            return;
        }
        final String seconds = String.format("%.1f", Math.max(remainingMillis, 100L) / 1000d);
        CivUI.notice(Kind.ERROR, MM.deserialize(template,
                Placeholder.component("remaining", CivText.value(seconds + "s"))))
            .from(plugin())
            .to(player)
            .on(Channel.ACTION_BAR)
            .send();
    }

    /**
     * Kept apart so CombatTagPlus's event class is only loaded when that plugin is present.
     */
    private final class CombatTagListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void standUpWhenTagged(@NotNull final PlayerCombatTagEvent event) {
            if (!config().isDismountOnCombatTag()) {
                return;
            }
            standUp(event.getVictim());
            standUp(event.getAttacker());
        }

        private void standUp(final Player player) {
            if (player != null && player.getVehicle() instanceof Cushion) {
                player.leaveVehicle();
            }
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rememberCushionDismount(@NotNull final EntityDismountEvent event) {
        if (event.getDismounted() instanceof Cushion && event.getEntity() instanceof Player player) {
            this.dismountTicks.put(player.getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    /**
     * The dismount teleport follows the dismount event within the same tick. If the spot it picks can't be seen
     * from where the player stood before sitting down, they're put back there instead.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventDismountThroughWalls(@NotNull final PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.DISMOUNT) {
            return;
        }
        final UUID uuid = event.getPlayer().getUniqueId();
        final Integer dismountTick = this.dismountTicks.remove(uuid);
        if (dismountTick == null || dismountTick != Bukkit.getCurrentTick()) {
            return;
        }
        final Location origin = this.mountedFrom.remove(uuid);
        if (origin == null || !config().isDismountThroughWallsBlocked()) {
            return;
        }
        final Location destination = event.getTo();
        if (origin.getWorld() != destination.getWorld() || isReachable(origin, destination)) {
            return;
        }
        final Location safe = origin.clone();
        safe.setYaw(destination.getYaw());
        safe.setPitch(destination.getPitch());
        event.setTo(safe);
    }

    private static boolean isReachable(final Location from, final Location to) {
        final Vector path = to.toVector().subtract(from.toVector());
        final double distance = path.length();
        if (distance < 1.0E-4d) {
            return true;
        }
        final Vector direction = path.normalize();
        for (final double height : RAY_HEIGHTS) {
            final Location start = from.clone().add(0d, height, 0d);
            if (from.getWorld().rayTraceBlocks(start, direction, distance,
                FluidCollisionMode.NEVER, true) != null) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void forgetCushionsOnLogout(@NotNull final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.lastMountTimes.remove(uuid);
        this.mountedFrom.remove(uuid);
        this.dismountTicks.remove(uuid);
    }

}
