package net.minelink.ctplus.task;

import net.minelink.ctplus.CombatTagPlus;
import net.minelink.ctplus.Tag;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minelink.ctplus.util.BarUtils;
import net.minelink.ctplus.util.Messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.Kind;

public final class TagUpdateTask extends BukkitRunnable {

    private final static Map<UUID, Integer> tasks = new HashMap<>();

    private final CombatTagPlus plugin;

    private final UUID playerId;

    private TagUpdateTask(CombatTagPlus plugin, Player player) {
        this.plugin = plugin;
        this.playerId = player.getUniqueId();
    }

    @Override
    public void run() {
        // Cancel if player went offline
        Player player = plugin.getPlayerCache().getPlayer(playerId);
        if (player == null) {
            cancel();
            return;
        }

        // Cancel if player is no longer tagged
        Tag tag = plugin.getTagManager().getTag(playerId);
        if (tag == null || tag.isExpired()) {
            if (plugin.getSettings().useBarApi()) {
                BarUtils.showBriefly(player, Messages.body(CivText.SUCCESS, plugin.getSettings().getBarApiEndedMessage()),
                    BossBar.Color.GREEN, 1);
            }

            Messages.send(player, Kind.SUCCESS, plugin.getSettings().getUntagMessage());
            cancel();
            return;
        }

        if (plugin.getSettings().useBarApi()) {
            int remainingDuration = tag.getTagDuration();
            int tagDuration = plugin.getSettings().getTagDuration();
            String remaining = plugin.getSettings().formatDuration(remainingDuration);

            // Display remaining timer in boss bar, updated in place
            Component name = Messages.body(CivText.WARN, plugin.getSettings().getBarApiCountdownMessage(),
                Messages.value("remaining", remaining));
            BarUtils.show(player, name, BossBar.Color.YELLOW, (float) remainingDuration / tagDuration);
        }
    }

    public static void run(final CombatTagPlus plugin, final Player p) {
        // Do nothing if player is a NPC
        if (plugin.getNpcPlayerHelper().isNpc(p)) return;

        final BukkitScheduler s = Bukkit.getScheduler();

        // Schedule the task to run on next tick
        s.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                // Do nothing if player isn't tagged or online
                if (!plugin.getTagManager().isTagged(p.getUniqueId()) || !p.isOnline()) {
                    return;
                }

                UUID playerId = p.getUniqueId();
                Integer taskId = tasks.get(playerId);

                // Do nothing if player already has an active task
                if (taskId != null && (s.isQueued(taskId) || s.isCurrentlyRunning(taskId))) {
                    return;
                }

                // Create new repeating task
                taskId = new TagUpdateTask(plugin, p).runTaskTimer(plugin, 0, 5).getTaskId();
                tasks.put(playerId, taskId);
            }
        });
    }

    public static void purgeFinished() {
        Iterator<Integer> iterator = tasks.values().iterator();
        BukkitScheduler s = Bukkit.getScheduler();

        // Loop over each task
        while (iterator.hasNext()) {
            int taskId = iterator.next();

            // Remove entry if task isn't running anymore
            if (!s.isQueued(taskId) && !s.isCurrentlyRunning(taskId)) {
                iterator.remove();
            }
        }
    }

    public static void cancelTasks(CombatTagPlus plugin) {
        Iterator<UUID> iterator = tasks.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = plugin.getPlayerCache().getPlayer(uuid);
            if (player != null && plugin.getSettings().useBarApi() && BarUtils.hasBar(player)) {
                BarUtils.removeBar(player);
            }

            int taskId = tasks.get(uuid);
            BukkitScheduler s = Bukkit.getScheduler();

            if (s.isQueued(taskId) || s.isCurrentlyRunning(taskId)) {
                s.cancelTask(taskId);
            }

            iterator.remove();
        }
    }

}
