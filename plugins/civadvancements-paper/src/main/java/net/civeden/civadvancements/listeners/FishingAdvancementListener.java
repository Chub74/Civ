package net.civeden.civadvancements.listeners;

import net.civeden.civadvancements.AdvancementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public FishingAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        adv.award(player, AdvancementManager.FIRST_FISH);
    }
}
