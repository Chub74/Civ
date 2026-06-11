package net.civeden.civadvancements.listeners;

import com.github.igotyou.FactoryMod.events.FactoryActivateEvent;
import net.civeden.civadvancements.AdvancementManager;
import net.civeden.civadvancements.ChallengeTracker;
import net.civeden.civadvancements.CivAdvancementsConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class FactoryAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;
    private final ChallengeTracker tracker;

    public FactoryAdvancementListener(AdvancementManager adv, CivAdvancementsConfig config, ChallengeTracker tracker) {
        this.adv = adv;
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactoryActivate(FactoryActivateEvent event) {
        Player player = event.getActivator();
        if (player == null) return; // redstone-triggered — no player to credit

        // Award factory_activate for any factory
        adv.award(player, AdvancementManager.FACTORY_ACTIVATE);

        // Award factory_create if this matches the configured beginner factory name
        String configured = config.getFactoryCreateName();
        if (!configured.isBlank()
                && event.getFactory().getName().equalsIgnoreCase(configured)) {
            adv.award(player, AdvancementManager.FACTORY_CREATE);
        }

        // Challenge milestones
        int runs = tracker.incrementFactoryRuns(player.getUniqueId());
        if (runs >= 100) adv.award(player, AdvancementManager.FACTORY_RUNS_100);
        if (runs >= 1000) adv.award(player, AdvancementManager.FACTORY_RUNS_1000);
        if (runs >= 10000) adv.award(player, AdvancementManager.FACTORY_RUNS_10K);
        if (runs >= 100000) adv.award(player, AdvancementManager.FACTORY_RUNS_100K);
    }
}
