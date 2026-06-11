package net.civeden.civadvancements.listeners;

import com.devotedmc.ExilePearl.event.PlayerPearledEvent;
import net.civeden.civadvancements.AdvancementManager;
import net.civeden.civadvancements.ChallengeTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ExilePearlAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final ChallengeTracker tracker;

    public ExilePearlAdvancementListener(AdvancementManager adv, ChallengeTracker tracker) {
        this.adv = adv;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearled(PlayerPearledEvent event) {
        Player player = event.getPearl().getPlayer();
        if (player == null) return;
        adv.award(player, AdvancementManager.GOT_PEARLED);
        int count = tracker.incrementPearled(player.getUniqueId());
        if (count >= 3) {
            adv.award(player, AdvancementManager.PEARLED_THRICE);
        }
    }
}
