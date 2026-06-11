package net.civeden.civadvancements.listeners;

import net.civeden.civadvancements.AdvancementManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ClayListener implements Listener {

    private final AdvancementManager adv;

    public ClayListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.CLAY) {
            adv.award(event.getPlayer(), AdvancementManager.CLAY_COLLECTED);
        }
    }
}
