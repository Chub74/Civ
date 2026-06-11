package net.civeden.civadvancements.listeners;

import com.untamedears.itemexchange.events.SuccessfulPurchaseEvent;
import net.civeden.civadvancements.AdvancementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ItemExchangeAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public ItemExchangeAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPurchase(SuccessfulPurchaseEvent event) {
        adv.award(event.getPurchaser(), AdvancementManager.FIRST_TRADE);
    }
}
