package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class CharcoalListener implements Listener {

    private final AdvancementManager adv;

    public CharcoalListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getItem().getItemStack().getType() == Material.CHARCOAL) {
            adv.award(player, AdvancementManager.CHARCOAL);
        }
    }
}
