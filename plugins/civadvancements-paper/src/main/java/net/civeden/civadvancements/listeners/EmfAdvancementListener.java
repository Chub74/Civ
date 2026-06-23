package net.civeden.civadvancements.listeners;

import com.oheers.fish.api.EMFFishEvent;
import net.civeden.civadvancements.AdvancementManager;
import net.civeden.civadvancements.ChallengeTracker;
import net.civeden.civadvancements.CivAdvancementsConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EmfAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final ChallengeTracker tracker;
    private final CivAdvancementsConfig config;

    public EmfAdvancementListener(AdvancementManager adv, ChallengeTracker tracker, CivAdvancementsConfig config) {
        this.adv = adv;
        this.tracker = tracker;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEmfFish(EMFFishEvent event) {
        Player player = event.getPlayer();
        adv.award(player, AdvancementManager.EMF_CATCH);
        if (event.getFish().getRarity().getAnnounce()) {
            adv.award(player, AdvancementManager.EMF_RARE);
            int rare = tracker.incrementEmfRareCatches(player.getUniqueId());
            if (rare >= 10) adv.award(player, AdvancementManager.EMF_RARE_10);
            if (rare >= 50) adv.award(player, AdvancementManager.EMF_RARE_50);
        }
        int count = tracker.incrementEmfCatches(player.getUniqueId());
        if (count >= config.getEmfLegendaryCount()) {
            adv.award(player, AdvancementManager.EMF_LEGENDARY);
        }
        if (count >= 250) adv.award(player, AdvancementManager.EMF_CATCH_250);
        if (count >= 1000) adv.award(player, AdvancementManager.EMF_CATCH_1000);
        if (count >= 5000) adv.award(player, AdvancementManager.EMF_CATCH_5000);
    }
}
