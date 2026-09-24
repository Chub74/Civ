package vg.civcraft.mc.citadel.listener;

import com.github.devotedmc.hiddenore.events.HiddenOreGenerateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vg.civcraft.mc.citadel.ReinforcementLogic;

/** Stops HiddenOre from turning reinforced blocks into ore. Only registered when HiddenOre is enabled. */
public class HiddenOreListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGenerate(HiddenOreGenerateEvent event) {
        if (ReinforcementLogic.getReinforcementAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}
