package vg.civcraft.mc.civmodcore.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLine;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLineAPI;

/**
 * One-shot action bar notices
 */
final class ActionBarNotices {

    /** How long a notice stays up */
    static final long SHOW_TICKS = 60L;

    private static final Map<Integer, BottomLine> LINES = new HashMap<>();
    private static final Map<String, BukkitTask> CLEARS = new HashMap<>();

    private ActionBarNotices() {
    }

    static void show(@NotNull final Player player, final int priority, @NotNull final Component message) {
        final BottomLine line = LINES.computeIfAbsent(priority,
            p -> BottomLineAPI.createBottomLine("civui-notice-" + p, p));
        // BottomLineAPI is String-based. Sprites can't survive that, so they're dropped for everyone here
        line.updatePlayer(player, LegacyComponentSerializer.legacySection().serialize(Bedrock.strip(message)));
        BottomLineAPI.refreshIndividually(player.getUniqueId());

        // A newer notice at the same priority replaces the old one and restarts its timer
        final UUID uuid = player.getUniqueId();
        final String key = uuid + "|" + priority;
        final BukkitTask previous = CLEARS.remove(key);
        if (previous != null) {
            previous.cancel();
        }
        CLEARS.put(key, Bukkit.getScheduler().runTaskLater(CivModCorePlugin.getInstance(), () -> {
            CLEARS.remove(key);
            line.removePlayer(uuid);
        }, SHOW_TICKS));
    }
}
