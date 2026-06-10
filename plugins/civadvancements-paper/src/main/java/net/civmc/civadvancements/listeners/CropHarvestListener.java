package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import net.civmc.civadvancements.ChallengeTracker;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class CropHarvestListener implements Listener {

    private final AdvancementManager adv;
    private final ChallengeTracker tracker;

    public CropHarvestListener(AdvancementManager adv, ChallengeTracker tracker) {
        this.adv = adv;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return;
        int count = tracker.incrementCropHarvest(event.getPlayer().getUniqueId());
        if (count >= 100) adv.award(event.getPlayer(), AdvancementManager.HARVEST_100);
        if (count >= 1000) adv.award(event.getPlayer(), AdvancementManager.HARVEST_1000);
        if (count >= 10000) adv.award(event.getPlayer(), AdvancementManager.HARVEST_10000);
        if (count >= 100000) adv.award(event.getPlayer(), AdvancementManager.HARVEST_100K);
        if (count >= 1000000) adv.award(event.getPlayer(), AdvancementManager.HARVEST_1M);
    }
}
