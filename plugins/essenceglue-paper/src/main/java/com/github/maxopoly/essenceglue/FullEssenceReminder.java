package com.github.maxopoly.essenceglue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/** Warns on join when stored essence is full, while there's still time to withdraw before today's reward. */
public class FullEssenceReminder implements Listener {

    private final EssenceGluePlugin plugin;
    private final VirtualEssenceManager manager;

    public FullEssenceReminder(EssenceGluePlugin plugin, VirtualEssenceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!manager.isAtEssenceCapacity(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            int stored = manager.getEssence(player.getUniqueId());
            if (plugin.getVoteManager() == null) {
                CivUI.warn(player, "Your essence is full at ", EssenceText.amount(stored), ". Take some out with ",
                    EssenceText.withdrawCommand(), " or you'll lose your next rewards");
            } else {
                CivUI.warn(player, "Your essence is full at ", EssenceText.amount(stored), ". Take some out with ",
                    EssenceText.withdrawCommand(), " before you vote, or you'll lose the rewards");
            }
        }, 20L);
    }
}
