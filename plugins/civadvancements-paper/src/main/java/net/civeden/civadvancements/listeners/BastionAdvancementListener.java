package net.civeden.civadvancements.listeners;

import isaac.bastion.event.BastionCreateEvent;
import isaac.bastion.event.BastionDamageEvent;
import net.civeden.civadvancements.AdvancementManager;
import net.civeden.civadvancements.CivAdvancementsConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BastionAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;

    public BastionAdvancementListener(AdvancementManager adv, CivAdvancementsConfig config) {
        this.adv = adv;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBastionCreate(BastionCreateEvent event) {
        adv.award(event.getPlayer(), AdvancementManager.PLACE_BASTION);
        String cityType = config.getBastionTypeName(AdvancementManager.BASTION_CITY);
        if (!cityType.isEmpty() && event.getBastion().getType().getName().equals(cityType)) {
            adv.award(event.getPlayer(), AdvancementManager.BASTION_CITY);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBastionDamage(BastionDamageEvent event) {
        adv.award(event.getPlayer(), AdvancementManager.BASTION_DENIED);
    }
}
