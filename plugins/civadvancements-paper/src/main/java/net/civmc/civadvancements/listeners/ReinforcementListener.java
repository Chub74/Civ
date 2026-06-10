package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import net.civmc.civadvancements.ChallengeTracker;
import net.civmc.civadvancements.CivAdvancementsConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vg.civcraft.mc.citadel.events.ReinforcementCreationEvent;

public class ReinforcementListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;
    private final ChallengeTracker tracker;

    public ReinforcementListener(AdvancementManager adv, CivAdvancementsConfig config, ChallengeTracker tracker) {
        this.adv = adv;
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReinforce(ReinforcementCreationEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        String typeName = event.getReinforcement().getType().getName();

        if (matches(typeName, AdvancementManager.REINFORCE_STONE)) {
            adv.award(player, AdvancementManager.REINFORCE_STONE);
        } else if (matches(typeName, AdvancementManager.REINFORCE_DEEPSLATE)) {
            adv.award(player, AdvancementManager.REINFORCE_DEEPSLATE);
        } else if (matches(typeName, AdvancementManager.REINFORCE_IRON)) {
            adv.award(player, AdvancementManager.REINFORCE_IRON);
        } else if (matches(typeName, AdvancementManager.REINFORCE_DIAMOND)) {
            adv.award(player, AdvancementManager.REINFORCE_DIAMOND);
        }

        // Challenge milestones — count all reinforcements regardless of tier
        int count = tracker.incrementReinforce(player.getUniqueId());
        if (count >= 100) adv.award(player, AdvancementManager.REINFORCE_100);
        if (count >= 1000) adv.award(player, AdvancementManager.REINFORCE_1000);
        if (count >= 10000) adv.award(player, AdvancementManager.REINFORCE_10K);
        if (count >= 100000) adv.award(player, AdvancementManager.REINFORCE_100K);
        if (count >= 1000000) adv.award(player, AdvancementManager.REINFORCE_1M);
    }

    private boolean matches(String typeName, String advKey) {
        String configured = config.getCitadelTypeName(advKey);
        return !configured.isBlank() && configured.equalsIgnoreCase(typeName);
    }
}
